package ru.skypro.homework.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.skypro.homework.component.AuthenticationComponent;
import ru.skypro.homework.dto.CommentDto;
import ru.skypro.homework.dto.ResponseWrapperCommentDto;
import ru.skypro.homework.exception.ActionForbiddenException;
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
import java.util.Optional;

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
    private final AuthenticationComponent auth;

    public CommentService(UserRepository userRepository,
                          AdvertRepository advertRepository,
                          CommentRepository commentRepository,
                          CommentMapper commentMapper,
                          AuthenticationComponent auth) {
        this.userRepository = userRepository;
        this.advertRepository = advertRepository;
        this.commentRepository = commentRepository;
        this.commentMapper = commentMapper;
        this.auth = auth;
    }

    /**
     * method for creating comment and save him into {@link CommentRepository}
     *
     * @param advertId   advert id
     * @param commentDto comment DTO object
     * @return commentDto
     */
    @Transactional
    public CommentDto create(Integer advertId, CommentDto commentDto) {
        log.info("creating comment:" + commentDto.getText() + " for advert with id: " + advertId);
        Advert advert = findAdvert(advertId);
        User user = userRepository.findByUsername(auth.getAuth().getName());
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
        Advert advert = findAdvert(advertId);
        Comment comment = findCommentWithAuth(advert, commentId);
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
        Advert advert = findAdvert(advertId);
        Comment comment = findCommentWithAuth(advert, commentId);
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

    private Advert findAdvert(int id) {
        return advertRepository.findById(id)
                .orElseThrow(() -> new AdvertNotFoundException("Advert not found"));
    }

    public Comment findCommentWithAuth(Advert advert, int id) {
        Optional<Comment> comment = commentRepository.findById(id);
        if (!comment.isPresent()) {
            throw new CommentNotFoundException("Comment not found");
        }
        if (comment.get().getAdvert().getId() != advert.getId()) {
            throw new CommentNotFoundException("Incorrect advert for comment");
        }
        if (auth.check(comment.get().getAuthor().getUsername())) {
            throw new ActionForbiddenException("Forbidden");
        }
        return comment.get();
    }
}
