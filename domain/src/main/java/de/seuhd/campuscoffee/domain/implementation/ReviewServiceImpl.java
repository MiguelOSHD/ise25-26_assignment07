package de.seuhd.campuscoffee.domain.implementation;

import de.seuhd.campuscoffee.domain.configuration.ApprovalConfiguration;
import de.seuhd.campuscoffee.domain.exceptions.ValidationException;
import de.seuhd.campuscoffee.domain.model.objects.Pos;
import de.seuhd.campuscoffee.domain.model.objects.Review;
import de.seuhd.campuscoffee.domain.model.objects.User;
import de.seuhd.campuscoffee.domain.ports.api.ReviewService;
import de.seuhd.campuscoffee.domain.ports.data.CrudDataService;
import de.seuhd.campuscoffee.domain.ports.data.PosDataService;
import de.seuhd.campuscoffee.domain.ports.data.ReviewDataService;
import de.seuhd.campuscoffee.domain.ports.data.UserDataService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of the Review service that handles business logic related to review entities.
 */
@Slf4j
@Service
public class ReviewServiceImpl extends CrudServiceImpl<Review, Long> implements ReviewService {
    private final ReviewDataService reviewDataService;
    private final UserDataService userDataService;
    private final PosDataService posDataService;
    // TODO: Try to find out the purpose of this class and how it is connected to the application.yaml configuration file.
    private final ApprovalConfiguration approvalConfiguration;

    public ReviewServiceImpl(@NonNull ReviewDataService reviewDataService,
                             @NonNull UserDataService userDataService,
                             @NonNull PosDataService posDataService,
                             @NonNull ApprovalConfiguration approvalConfiguration) {
        super(Review.class);
        this.reviewDataService = reviewDataService;
        this.userDataService = userDataService;
        this.posDataService = posDataService;
        this.approvalConfiguration = approvalConfiguration;
    }

    @Override
    protected CrudDataService<Review, Long> dataService() {
        return reviewDataService;
    }

    @Override
    @Transactional
    public @NonNull Review upsert(@NonNull Review review) {
        // TODO: Implement the missing business logic here
        User author = review.author();
        Long authorId = author.getId();
        if (author == null || authorId == null) {
            throw new ValidationException("Review has to have a valid author.");
        }
        userDataService.getById(authorId);

        Pos pos = review.pos();
        Long posId = pos.getId();
        if ( pos == null || posId == null) {
            throw new ValidationException("Review has to reference a valid POS.");
        }
        Pos persistedPos = posDataService.getById(posId);
        if(persistedPos == null){
            throw new ValidationException("POS does not exist");
        }
        Long reviewId = review.getId();

        List<Review> existing = reviewDataService.filter(persistedPos, author);
        if (!existing.isEmpty()){
            boolean conflict;
            if (reviewId == null) {
                conflict = true;
            } else {
                conflict = existing.stream()
                        .anyMatch(r -> r.getId() == null || !r.getId().equals(reviewId));
            }
            if (conflict) {
                throw new ValidationException("A user cannot review a POS more than once");
            }
        }
        Integer count = review.approvalCount();
        if(count == null){
            throw new ValidationException("Count has to be valid.");
        }
        Review updated = review.toBuilder()
                .approvalCount(count)
                .build();

        review = updateApprovalStatus(updated);

        Review upserted = reviewDataService.upsert(review);
        if(upserted == null){
            throw new ValidationException("Upsert has to be valid");
        }
        return upserted;
        // return super.upsert(review);
    }

    @Override
    @Transactional(readOnly = true)
    public @NonNull List<Review> filter(@NonNull Long posId, @NonNull Boolean approved) {
        return reviewDataService.filter(posDataService.getById(posId), approved);
    }

    @Override
    @Transactional
    public @NonNull Review approve(@NonNull Review review, @NonNull Long userId) {
        log.info("Processing approval request for review with ID '{}' by user with ID '{}'...",
                review.getId(), userId);

        // validate that the user exists
        // TODO: Implement the required business logic here
        User user = userDataService.getById(userId);
        if (user == null) {
            throw new ValidationException("Benutzer existiert nicht.");
        }

        // validate that the review exists
        // TODO: Implement the required business logic here
        Long reviewId = review.getId();
        if(reviewId == null){
            throw new ValidationException("Review must exist");
        }
        Review existing = reviewDataService.getById(reviewId);
        if (existing == null) {
            throw new ValidationException("Review does not exist.");
        }

        // a user cannot approve their own review
        // TODO: Implement the required business logic here
        User author = existing.author();
        Long authorId = author.getId();
        if (authorId != null && authorId.equals(user.getId())) {
            throw new ValidationException("User cannot approve own review.");
        }

        // increment approval count
        // TODO: Implement the required business logic here
        Integer currentCount = existing.approvalCount();
        Review updated = existing.toBuilder()
                .approvalCount(currentCount + 1)
                .build();

        // update approval status to determine if the review now reaches the approval quorum
        // TODO: Implement the required business logic here
        review = updateApprovalStatus(updated);

        return reviewDataService.upsert(review);
    }

    /**
     * Calculates and updates the approval status of a review based on the approval count.
     * Business rule: A review is approved when it reaches the configured minimum approval count threshold.
     *
     * @param review The review to calculate approval status for
     * @return The review with updated approval status
     */
    Review updateApprovalStatus(Review review) {
        log.debug("Updating approval status of review with ID '{}'...", review.getId());
        return review.toBuilder()
                .approved(isApproved(review))
                .build();
    }
    
    /**
     * Determines if a review meets the minimum approval threshold.
     * 
     * @param review The review to check
     * @return true if the review meets or exceeds the minimum approval count, false otherwise
     */
    private boolean isApproved(Review review) {
        return review.approvalCount() >= approvalConfiguration.minCount();
    }
}
