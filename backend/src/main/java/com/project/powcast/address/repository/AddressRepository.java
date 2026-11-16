package com.project.powcast.address.repository;

import com.project.powcast.address.entity.Addresses;
import com.project.powcast.member.entity.Members;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Addresses, Long> {
    List<Addresses>findAllByMember_Id(Long memberId);
    Optional<Addresses> findByIdAndMemberId(Long addressId, Long memberId);
}
