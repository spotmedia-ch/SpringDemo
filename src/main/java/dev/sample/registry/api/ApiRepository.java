package dev.sample.registry.api;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface ApiRepository extends JpaRepository<Api, UUID>, JpaSpecificationExecutor<Api> {

}
