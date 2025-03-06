package com.urakata.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.urakata.model.ServerDetails;

@Repository
public interface IServerDetailsRepository
		extends JpaRepository<ServerDetails, Long>, JpaSpecificationExecutor<ServerDetails> {

	Optional<ServerDetails> findByServerName(String string);

}
