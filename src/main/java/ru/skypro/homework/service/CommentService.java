package ru.skypro.homework.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.skypro.homework.dto.CommentDto;
import ru.skypro.homework.dto.ResponseWrapperCommentDto;
import ru.skypro.homework.exception.AdvertNotFoundException;
import ru.skypro.homework.exception.CommentNotFoundException;
import ru.skypro.homework.mapper.CommentMapper;
import ru.skypro.homework.model.Advert;
import ru.skypro.homework.model.Comment;
import ru.skypro.homework.model.User;
import ru.skypro.homework.repository.AdvertRepository;
import ru.skypro.homework.repository.CommentRepository;
import ru.skypro.homework.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for creating, deleting, updating, finding comments from {@link CommentRepository}
 */
@Service
@Slf4j
public class CommentService {
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final AdvertRepository advertRepository;
    private final CommentMapper commentMapper;

    public CommentService(UserRepository userRepository,
                          AdvertRepository advertRepository,
                          CommentRepository commentRepository,
                          CommentMapper commentMapper) {
        this.userRepository = userRepository;
        this.advertRepository = advertRepository;
        this.commentRepository = commentRepository;
        this.commentMapper = commentMapper;
    }

    /**
     * method for creating comment and save him into {@link CommentRepository}
     *
     * @param advertId   advert id
     * @param commentDto comment DTO object
     * @return commentDto
     */
    @Transactional
    public CommentDto create(Authentication auth, Integer advertId, CommentDto commentDto) {
        log.info("creating comment:" + commentDto.getText() + " for advert with id: " + advertId);
        Advert advert = advertRepository.findById(advertId)
                .orElseThrow(() -> new AdvertNotFoundException("Advert not found"));
        User user = userRepository.findByEmail(auth.getName());
        Comment comment = commentMapper.commentDtoToComment(commentDto);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setAuthor(user);
        comment.setAdvert(advert);
        commentRepository.save(comment);
        return commentMapper.commentToCommentDto(comment);
    }

    /**
     * method for deleting comment from db by advertId and commentId
     *
     * @param advertId  advert id
     * @param commentId comment id
     */
    @Transactional
    public void delete(Integer advertId, Integer commentId) {
        log.info("deleting comment with id: " + commentId);
        Advert advert = advertRepository.findById(advertId)
                .orElseThrow(() -> new AdvertNotFoundException("Advert not found"));
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException("Comment not found"));
        if (comment.getAdvert().getId() != advert.getId()) {
            throw new CommentNotFoundException("Incorrect advert for comment");
        }
        commentRepository.delete(comment);
    }

    /**
     * method for updating comment
     *
     * @param advertId   advert id
     * @param commentId  comment id
     * @param commentDto comment DTO object
     */
    @Transactional
    public CommentDto update(Integer advertId, Integer commentId, CommentDto commentDto) {
        log.info("updating comment with id: " + advertId);
        Advert advert = advertRepository.findById(advertId)
                .orElseThrow(() -> new AdvertNotFoundException("Advert not found"));
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException("Comment not found"));
        if (comment.getAdvert().getId() != advert.getId()) {
            throw new CommentNotFoundException("Incorrect advert for comment");
        }
        commentMapper.updateComment(commentDto, comment);
        commentRepository.save(comment);
        return commentMapper.commentToCommentDto(comment);
    }

    /**
     * method for getting number and list of comments by advertId
     *
     * @param advertId advert id
     * @return responseWrapperCommentDto contains number of comments and list of comments
     */
    public ResponseWrapperCommentDto findAll(Integer advertId) {
        log.info("getting all comments for advert with id: " + advertId);
        List<Comment> comments = commentRepository.findAllByAdvertId(advertId);
        return commentMapper.listToRespWrapperCommentDto(comments);
    }
}