package Emerging.App.Backend.Repository;

import Emerging.App.Backend.Entities.SentApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SentApplicationRepository extends JpaRepository<SentApplication, Integer> {

    // For Students
    @Query("SELECT u.createdApplication.applicationId FROM SentApplication u WHERE u.sender.userId = :userId")
    List<Integer> getSentApplicationsCreatedApplicationId(@Param("userId") int userId);

    @Query("SELECT u FROM SentApplication u WHERE u.createdApplication.applicationId = :createdApplicationId")
    List<SentApplication> getStudentsSentApplicationFromCreatedApplication(@Param("createdApplicationId") int createdApplicationId);

    @Query("SELECT u FROM SentApplication u WHERE u.sender.userId = :userId")
    List<SentApplication> getStudentApplicationsList(@Param("userId") int userId);

    // For Faculty
    @Query("SELECT u FROM SentApplication u WHERE u.receiver.userId = :userId")
    List<SentApplication> getReceivedApplicationsList(@Param("userId") int userId);

    @Query("SELECT u FROM SentApplication u WHERE u.receiver.userId = :userId AND CONCAT(u.sender.userDetails.firstName, ' ', u.sender.userDetails.lastName) LIKE :query")
    List<SentApplication> getSearchQueryResults(@Param("userId") int userId, @Param("query") String query);

}
