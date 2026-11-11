package dev.sample.registry.consumer;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface ConsumerRepository extends JpaRepository<Consumer, UUID> {

}
