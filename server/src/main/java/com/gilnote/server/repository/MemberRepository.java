package com.gilnote.server.repository;

import com.gilnote.server.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByMemberToken(String memberToken);
}