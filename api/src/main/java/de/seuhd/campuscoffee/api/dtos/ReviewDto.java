package de.seuhd.campuscoffee.api.dtos;

import de.seuhd.campuscoffee.domain.model.objects.Pos;
import de.seuhd.campuscoffee.domain.model.objects.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * DTO record for POS metadata.
 */
@Builder(toBuilder = true)
public record ReviewDto (
    @Nullable Long id,
    // TODO: Implement ReviewDto
    @Nullable LocalDateTime createdAt,
    @Nullable LocalDateTime updatedAt,

    @NotNull
    @NonNull Pos pos,

    @NotNull
    @NonNull User author,

    @NotNull
    @NonNull Long posId,

    @NotNull
    @NonNull Long authorId,

    @NotBlank(message = "Review cannot be empty.")
    @NonNull String review,

    @NotNull
    @NonNull Boolean approved
    ) implements Dto<Long> {
    @Override
    public @Nullable Long getId() {
        return id;
    }

    public @Nullable Long posId() { return pos.getId(); }

    public @Nullable Long authorId() {return author.getId();}

}
