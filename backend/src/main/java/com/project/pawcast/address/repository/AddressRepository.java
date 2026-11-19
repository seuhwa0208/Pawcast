package com.project.pawcast.address.repository;

import com.project.pawcast.address.entity.Addresses;
import com.project.pawcast.member.entity.Members;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Addresses, Long> {
    List<Addresses>findAllByMember_Id(Long memberId);
    Optional<Addresses> findByIdAndMemberId(Long addressId, Long memberId);
}
