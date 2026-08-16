package com.t4kash.api.network.controller;

import com.t4kash.api.identity.dto.AuthenticatedUserResponse;
import com.t4kash.api.identity.web.CurrentUser;
import com.t4kash.api.network.dto.CommentResponse;
import com.t4kash.api.network.dto.CreateCommentRequest;
import com.t4kash.api.network.dto.CreatePostRequest;
import com.t4kash.api.network.dto.PostResponse;
import com.t4kash.api.network.dto.ReactionRequest;
import com.t4kash.api.network.dto.UpdateCommentRequest;
import com.t4kash.api.network.dto.UpdatePostRequest;
import com.t4kash.api.network.service.NetworkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/network")
@Tag(name = "Network", description = "Feed, publicaciones e interacciones sociales")
@SecurityRequirement(name = "bearerAuth")
public class NetworkController {
    private final NetworkService networkService;

    public NetworkController(NetworkService networkService) {
        this.networkService = networkService;
    }

    @GetMapping("/feed")
    @Operation(summary = "Consultar el feed social")
    public List<PostResponse> listFeed(
            @CurrentUser AuthenticatedUserResponse user,
            @RequestParam(defaultValue = "PARA_TI") String alcance,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return networkService.listFeed(
                user.idUsuario(),
                alcance,
                page,
                size
        );
    }

    @GetMapping("/saved")
    @Operation(summary = "Consultar mis publicaciones guardadas")
    public List<PostResponse> listSaved(
            @CurrentUser AuthenticatedUserResponse user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return networkService.listSaved(user.idUsuario(), page, size);
    }

    @GetMapping("/posts/{idPublicacion}")
    @Operation(summary = "Consultar una publicacion visible")
    public PostResponse getPost(
            @CurrentUser AuthenticatedUserResponse user,
            @PathVariable Integer idPublicacion
    ) {
        return networkService.getPost(user.idUsuario(), idPublicacion);
    }

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear una publicacion")
    public PostResponse createPost(
            @CurrentUser AuthenticatedUserResponse user,
            @Valid @RequestBody CreatePostRequest request
    ) {
        return networkService.createPost(user.idUsuario(), request);
    }

    @PutMapping("/posts/{idPublicacion}")
    @Operation(summary = "Editar una publicacion propia")
    public PostResponse updatePost(
            @CurrentUser AuthenticatedUserResponse user,
            @PathVariable Integer idPublicacion,
            @Valid @RequestBody UpdatePostRequest request
    ) {
        return networkService.updatePost(
                user.idUsuario(),
                idPublicacion,
                request
        );
    }

    @DeleteMapping("/posts/{idPublicacion}")
    @Operation(summary = "Eliminar una publicacion propia")
    public PostResponse deletePost(
            @CurrentUser AuthenticatedUserResponse user,
            @PathVariable Integer idPublicacion
    ) {
        return networkService.deletePost(user.idUsuario(), idPublicacion);
    }

    @PutMapping("/posts/{idPublicacion}/reaction")
    @Operation(summary = "Agregar o cambiar mi reaccion")
    public PostResponse setReaction(
            @CurrentUser AuthenticatedUserResponse user,
            @PathVariable Integer idPublicacion,
            @Valid @RequestBody ReactionRequest request
    ) {
        return networkService.setReaction(
                user.idUsuario(),
                idPublicacion,
                request.tipoReaccion()
        );
    }

    @DeleteMapping("/posts/{idPublicacion}/reaction")
    @Operation(summary = "Quitar mi reaccion")
    public PostResponse removeReaction(
            @CurrentUser AuthenticatedUserResponse user,
            @PathVariable Integer idPublicacion
    ) {
        return networkService.removeReaction(user.idUsuario(), idPublicacion);
    }

    @PutMapping("/posts/{idPublicacion}/saved")
    @Operation(summary = "Guardar una publicacion")
    public PostResponse savePost(
            @CurrentUser AuthenticatedUserResponse user,
            @PathVariable Integer idPublicacion
    ) {
        return networkService.savePost(user.idUsuario(), idPublicacion);
    }

    @DeleteMapping("/posts/{idPublicacion}/saved")
    @Operation(summary = "Quitar una publicacion de guardadas")
    public PostResponse removeSavedPost(
            @CurrentUser AuthenticatedUserResponse user,
            @PathVariable Integer idPublicacion
    ) {
        return networkService.removeSavedPost(user.idUsuario(), idPublicacion);
    }

    @GetMapping("/posts/{idPublicacion}/comments")
    @Operation(summary = "Listar comentarios de una publicacion")
    public List<CommentResponse> listComments(
            @CurrentUser AuthenticatedUserResponse user,
            @PathVariable Integer idPublicacion,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return networkService.listComments(
                user.idUsuario(),
                idPublicacion,
                page,
                size
        );
    }

    @PostMapping("/posts/{idPublicacion}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Comentar o responder una publicacion")
    public CommentResponse createComment(
            @CurrentUser AuthenticatedUserResponse user,
            @PathVariable Integer idPublicacion,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        return networkService.createComment(
                user.idUsuario(),
                idPublicacion,
                request
        );
    }

    @PutMapping("/comments/{idComentario}")
    @Operation(summary = "Editar un comentario propio")
    public CommentResponse updateComment(
            @CurrentUser AuthenticatedUserResponse user,
            @PathVariable Integer idComentario,
            @Valid @RequestBody UpdateCommentRequest request
    ) {
        return networkService.updateComment(
                user.idUsuario(),
                idComentario,
                request
        );
    }

    @DeleteMapping("/comments/{idComentario}")
    @Operation(summary = "Eliminar un comentario propio")
    public CommentResponse deleteComment(
            @CurrentUser AuthenticatedUserResponse user,
            @PathVariable Integer idComentario
    ) {
        return networkService.deleteComment(user.idUsuario(), idComentario);
    }
}
