package com.project.powcast.product.service;
import com.project.powcast.common.exception.*;
import com.project.powcast.member.entity.Members;
import com.project.powcast.member.repository.MemberRepository;
import com.project.powcast.security.AuthorizationValidator;
import com.project.powcast.product.dto.*;
import com.project.powcast.product.entity.*;
import com.project.powcast.product.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import java.time.LocalDateTime;
import java.util.*;
@Service
@RequiredArgsConstructor
@Transactional(readOnly=true)
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final FoodDetailRepository foodRepository;
    private final DeviceDetailRepository deviceRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryTransactionRepository inventoryRepository;
    private final MemberRepository memberRepository;
    private final AuthorizationValidator authorizationValidator;
    private final ProductCreateValidator validator;
    private final ProductReviewRepository reviewRepository;

    private Members manager(Long memberId) {
        if (memberId==null) throw new BusinessException(ErrorCode.FORBIDDEN);
        Members member=memberRepository.findById(memberId).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        if (member.getStatus()!=Members.Status.ACTIVE) throw new BusinessException(ErrorCode.FORBIDDEN);
        authorizationValidator.validateManager(member);
        return member;
    }
    public Product findById(Long id) {
        return productRepository.findManagementProductById(id).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND_PRODUCT));
    }
    private Category category(Long id) {
        return categoryRepository.findById(id).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND_CATEGORY));
    }
    public List<ProductResponse> searchProducts(ProductSearchRequest r) {
        validator.validate(r);
        if (r.minPrice()!=null && r.maxPrice()!=null && r.minPrice().compareTo(r.maxPrice())>0) throw new BusinessException(ErrorCode.INVALID_PRICE_RANGE);
        return productRepository.searchProducts(r.keyword(),r.productType(),r.categoryId(),r.brand(),r.ingredient(),r.foodKind(),r.foodForm(),r.lifeStage(),r.minPrice(),r.maxPrice(),r.minRating(),r.inStockOnly(),r.sortType()).stream().map(ProductResponse::from).toList();
    }
    public List<ProductResponse> managementList(Long memberId) {
        manager(memberId);
        return productRepository.findAllByOrderByIdAsc().stream().map(p -> {
            var variants=variantRepository.findAllByProductIdOrderByIdAsc(p.getId());
            var v=variants.stream().min(Comparator.comparing(ProductVariant::getSalePrice).thenComparing(ProductVariant::getId)).orElse(null);
            var images=imageRepository.findAllByProductIdOrderBySortOrderAscIdAsc(p.getId());
            return new ProductResponse(p.getId(),p.getName(),p.getBrand(),p.getCategory()==null?null:p.getCategory().getName(),images.isEmpty()?null:images.getFirst().getImagePath(),v==null?null:v.getId(),v==null?null:v.getPrice(),v==null?null:v.getSalePrice(),v==null?null:v.getDiscountRate(),p.getSalesCount(),p.getWishCount(),p.getReviewCount(),p.getReviewCount()==0?java.math.BigDecimal.ZERO.setScale(1):java.math.BigDecimal.valueOf(p.getRatingSum()).divide(java.math.BigDecimal.valueOf(p.getReviewCount()),1,java.math.RoundingMode.HALF_UP),variants.stream().noneMatch(x->x.getProductVariantStatus()==ProductVariantStatus.ON_SALE && x.getStock()>0));
        }).toList();
    }
    public ProductReviewsResponse reviews(Long id) {
        detail(id);
        var reviews=reviewRepository.findVisibleReviews(id).stream().map(BestReviewResponse::from).toList();
        var average=reviews.isEmpty()?java.math.BigDecimal.ZERO.setScale(1):java.math.BigDecimal.valueOf(reviews.stream().mapToLong(BestReviewResponse::rating).sum()).divide(java.math.BigDecimal.valueOf(reviews.size()),1,java.math.RoundingMode.HALF_UP);
        return new ProductReviewsResponse(reviews.size(),average,reviews);
    }
    public List<BestReviewProductResponse> bestReviewProducts(ProductSearchRequest request) {
        var products=searchProducts(request).stream().filter(p->p.reviewCount()>0).sorted(Comparator.comparing(ProductResponse::averageRating).reversed().thenComparing(ProductResponse::id)).limit(5).toList();
        if(products.isEmpty()) return List.of();
        var best=reviewRepository.findBestReviews(products.stream().map(ProductResponse::id).toList()).stream().collect(java.util.stream.Collectors.toMap(ProductReviewView::getProductId, BestReviewResponse::from));
        return products.stream().map(p->new BestReviewProductResponse(p,best.get(p.id()))).toList();
    }
    public ProductDetailResponse detail(Long id) {
        Product p=productRepository.findPublicProductById(id).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND_PRODUCT));
        return response(p,false);
    }
    public ProductDetailResponse managementDetail(Long memberId, Long id) { manager(memberId); return response(findById(id),true); }
    private ProductDetailResponse response(Product p, boolean management) {
        var variants=variantRepository.findAllByProductIdOrderByIdAsc(p.getId()).stream().filter(v->management || v.getProductVariantStatus()!=ProductVariantStatus.DISCONTINUED).toList();
        return ProductDetailResponse.from(p,variants,imageRepository.findAllByProductIdOrderBySortOrderAscIdAsc(p.getId()),foodRepository.findById(p.getId()).orElse(null),deviceRepository.findById(p.getId()).orElse(null));
    }
    public List<CategoryResponse> categories() { return categoryRepository.findAllByOrderByIdAsc().stream().map(CategoryResponse::from).toList(); }
    public List<String> brands() { return productRepository.findBrands(); }
    @Transactional
    public ProductDetailResponse create(Long memberId, ProductCreateRequest r) {
        manager(memberId); validator.validateCreate(r);
        Product p=productRepository.save(Product.builder().name(r.name().strip()).brand(r.brand()).productType(r.productType()).category(category(r.categoryId())).description(r.description()).build());
        for (var v:r.variants()) addVariant(p,v);
        replaceImages(p,r.images());
        saveDetails(p,r.foodDetail(),r.deviceDetail());
        return response(p,true);
    }
    private ProductVariant addVariant(Product p, VariantCreateRequest r) {
        validator.validate(r);
        validator.validateVariant(p.getProductType(),r.color(),r.weightG(),r.volumeMl(),r.capacityMl(),r.heightCm());
        if (variantRepository.existsBySkuCode(r.skuCode())) throw new BusinessException(ErrorCode.DUPLICATE_SKU);
        var v=ProductVariant.builder().product(p).skuCode(r.skuCode()).optionKey(r.optionKey()).optionLabel(r.optionLabel()).color(r.color()).weightG(r.weightG()).volumeMl(r.volumeMl()).capacityMl(r.capacityMl()).heightCm(r.heightCm()).price(r.price()).costPrice(r.costPrice()).discountRate(r.discountRate()).stock(r.stock()).productVariantStatus(r.stock()==0?ProductVariantStatus.SOLD_OUT:ProductVariantStatus.ON_SALE).build();
        try { return variantRepository.saveAndFlush(v); }
        catch (DataIntegrityViolationException e) { throw new BusinessException(ErrorCode.PRODUCT_DATA_CONFLICT); }
    }
    @Transactional
    public ProductDetailResponse update(Long memberId, Long id, ProductUpdateRequest r) {
        manager(memberId); validator.validate(r); validator.validateImages(r.images());
        Product p=findById(id);
        if (r.name()!=null) { if(r.name().isBlank()) throw new BusinessException(ErrorCode.INVALID_PRODUCT_REQUEST); p.setName(r.name().strip()); }
        if (r.brand()!=null) p.setBrand(r.brand());
        if (r.categoryId()!=null) p.setCategory(category(r.categoryId()));
        if (r.description()!=null) p.setDescription(r.description());
        if (r.images()!=null) replaceImages(p,r.images());
        if (p.getProductType()==ProductType.FOOD && r.deviceDetail()!=null || p.getProductType()==ProductType.DEVICE && r.foodDetail()!=null) throw new BusinessException(ErrorCode.INVALID_PRODUCT_DETAIL);
        saveDetails(p,r.foodDetail(),r.deviceDetail());
        return response(p,true);
    }
    private void saveDetails(Product p, FoodDetailCreateRequest f, DeviceDetailCreateRequest d) {
        if (f!=null) foodRepository.saveAndFlush(FoodDetail.builder().productId(p.getId()).product(p).foodKind(f.foodKind()).foodForm(f.foodForm()).mainIngredient(f.mainIngredient()).lifeStage(f.lifeStage()).ingredientDescription(f.ingredientDescription()).build());
        if (d!=null) deviceRepository.saveAndFlush(DeviceDetail.builder().productId(p.getId()).product(p).deviceKind(d.deviceKind()).material(d.material()).featureDescription(d.featureDescription()).build());
    }
    private void replaceImages(Product p, List<ImageCreateRequest> images) {
        imageRepository.deleteAllByProductId(p.getId()); imageRepository.flush();
        imageRepository.saveAll(images.stream().map(i->ProductImage.builder().product(p).imagePath(i.imagePath()).main(i.main()).sortOrder(i.sortOrder()).build()).toList());
    }
    @Transactional
    public ProductDetailResponse createVariant(Long memberId, Long id, VariantCreateRequest r) { manager(memberId); Product p=findById(id); addVariant(p,r); return response(p,true); }
    @Transactional
    public ProductDetailResponse updateVariant(Long memberId, Long productId, Long variantId, VariantUpdateRequest r) {
        manager(memberId); validator.validate(r);
        var v=variantRepository.findByIdForUpdate(variantId).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND_PRODUCT_VARIANT));
        if(!v.getProduct().getId().equals(productId)) throw new BusinessException(ErrorCode.NOT_FOUND_PRODUCT_VARIANT);
        validator.validateVariant(v.getProduct().getProductType(),r.color(),r.weightG(),r.volumeMl(),r.capacityMl(),r.heightCm());
        if(variantRepository.existsBySkuCodeAndIdNot(r.skuCode(),v.getId())) throw new BusinessException(ErrorCode.DUPLICATE_SKU);
        if(r.status()==ProductVariantStatus.ON_SALE && v.getStock()==0) throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        v.setSkuCode(r.skuCode()); v.setOptionKey(r.optionKey()); v.setOptionLabel(r.optionLabel()); v.setColor(r.color()); v.setWeightG(r.weightG()); v.setVolumeMl(r.volumeMl()); v.setCapacityMl(r.capacityMl()); v.setHeightCm(r.heightCm()); v.setPrice(r.price()); v.setCostPrice(r.costPrice()); v.setDiscountRate(r.discountRate()); v.setProductVariantStatus(r.status());
        try { variantRepository.flush(); } catch(DataIntegrityViolationException e) { throw new BusinessException(ErrorCode.PRODUCT_DATA_CONFLICT); }
        return response(v.getProduct(),true);
    }
    @Transactional
    public void discontinue(Long memberId, Long productId) {
        manager(memberId); findById(productId);
        var ids=variantRepository.findAllByProductIdOrderByIdAsc(productId).stream().map(ProductVariant::getId).toList();
        if(!ids.isEmpty()) variantRepository.findAllByIdForUpdate(ids).forEach(v->v.setProductVariantStatus(ProductVariantStatus.DISCONTINUED));
    }
    @Transactional
    public StockAdjustmentResponse adjustStock(Long memberId, Long variantId, StockAdjustmentRequest r) {
        var member=manager(memberId); validator.validate(r);
        var v=variantRepository.findByIdForUpdate(variantId).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND_PRODUCT_VARIANT));
        long after=(long)v.getStock()+r.quantityChange();
        if(r.quantityChange()==0 || after<0 || after>Integer.MAX_VALUE) throw new BusinessException(ErrorCode.INVALID_STOCK_ADJUSTMENT);
        int before=v.getStock(); v.setStock((int)after);
        if(v.getProductVariantStatus()!=ProductVariantStatus.DISCONTINUED) v.setProductVariantStatus(after==0?ProductVariantStatus.SOLD_OUT:ProductVariantStatus.ON_SALE);
        return StockAdjustmentResponse.from(inventoryRepository.save(InventoryTransaction.builder().variant(v).member(member).transactionType("ADJUSTMENT").quantityChange(r.quantityChange()).stockBefore(before).stockAfter((int)after).referenceType(r.reason()).createdAt(LocalDateTime.now()).build()));
    }
    public List<StockAdjustmentResponse> stockHistory(Long memberId, Long productId) {
        manager(memberId); findById(productId);
        return inventoryRepository.findAllByVariantProductIdOrderByCreatedAtDescIdDesc(productId).stream().map(StockAdjustmentResponse::from).toList();
    }
}
