package com.gilnote.server.repository;

import com.gilnote.server.domain.Car;
import com.gilnote.server.domain.Member;
import com.gilnote.server.domain.Note;
import com.gilnote.server.domain.NoteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {

    // API 5: 내가 보낸 쪽지, 최신순
    List<Note> findBySenderOrderByCreatedAtDesc(Member sender);

    // API 6: 이 차의 아직 안 들은 쪽지, 오래된 순
    List<Note> findByCarAndStatusInOrderByCreatedAtAsc(Car car, List<NoteStatus> statuses);
}
