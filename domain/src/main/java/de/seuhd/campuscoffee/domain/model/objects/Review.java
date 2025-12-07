package de.seuhd.campuscoffee.domain.model.objects;

import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * Domain record that stores a review for a point of sale.
 * Reviews are approved once they received a configurable number of approvals.
 */
@Builder(toBuilder = true)
public record Review(
        @Nullable Long id, // null when the review has not been created yet
        //TODO: Implement review domain model.
        @Nullable LocalDateTime createdAt, // set on review creation
        @Nullable LocalDateTime updatedAt, // set on review creation and update
        @NonNull Pos pos, // POS the review is referring to
        @NonNull User author, // Author of the review
        @NonNull String review, // The actual text of the review
        @NonNull Integer approvalCount, // is updated by the domain module
        @NonNull Boolean approved // is determined by the domain module
) implements DomainModel<Long> {
    @Override
    public Long getId() {
        return id;
    }

    // Implemented posId() and authorId() as methods because ReviewServiceTest and TestFixtures requires .pos() and .author()
    // and TestFixtures requires .pos() and .author() in the Builder, so it is kinda
    // redundant to also have the posId and authorId as members.
    // Also, the ReviewDtoMapper needs those methods.
    public Long posId() {
        return pos.getId();
    }
    public Long authorId() {
        return author.getId();
    }
}