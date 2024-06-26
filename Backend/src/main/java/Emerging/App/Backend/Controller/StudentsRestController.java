package Emerging.App.Backend.Controller;

import Emerging.App.Backend.Entities.*;
import Emerging.App.Backend.JSON_Objects.Authentication.ApplicationRequest;
import Emerging.App.Backend.JSON_Objects.Faculty.FacultyApplicationResponse;
import Emerging.App.Backend.JSON_Objects.Students.Details;
import Emerging.App.Backend.JSON_Objects.Students.StudentApplicationListResponse;
import Emerging.App.Backend.JSON_Objects.Students.StudentApplicationResponse;
import Emerging.App.Backend.Repository.CreatedApplicationRepository;
import Emerging.App.Backend.Repository.SentApplicationRepository;
import Emerging.App.Backend.Repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/students")
public class StudentsRestController {

    private CreatedApplicationRepository createdApplicationRepository;

    private SentApplicationRepository sentApplicationRepository;

    private UsersRepository usersRepository;

    @Autowired
    public StudentsRestController(SentApplicationRepository sentApplicationRepository, CreatedApplicationRepository createdApplicationRepository, UsersRepository usersRepository){
        this.usersRepository = usersRepository;
        this.createdApplicationRepository = createdApplicationRepository;
        this.sentApplicationRepository = sentApplicationRepository;
    }

    @GetMapping("/created-applications-list")
    public ResponseEntity<?> createdApplication(@RequestParam(name = "search-query", required = false) String searchQuery){
        Users user = getUser();
        if(user == null){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        List<Integer> sentCreatorIds = sentApplicationRepository.getSentApplicationsCreatedApplicationId(user.getUserId());
        List<StudentApplicationListResponse> responseList = new ArrayList<>();

        if(searchQuery != null){
            List<CreatedApplication> queriedCreatedApplicationList = createdApplicationRepository.findByQuery("%" + searchQuery + "%");
            Iterator<CreatedApplication> iterator = queriedCreatedApplicationList.listIterator();

            while(iterator.hasNext()){
                CreatedApplication current =  iterator.next();
                String professorName = current.getUser().getUserDetails().getFirstName() + " " + current.getUser().getUserDetails().getLastName();
                String department = current.getUser().getUserDetails().getDepartment();
                String projectName = current.getName();
                String status = "NOT SUBMITTED";
                String decision = "SUBMIT APP TO RECEIVE DECISION";
                List<SentApplication> sentApplication;
                int sentApplicationId = -1;
                if (sentCreatorIds.contains(current.getApplicationId())) {
                    status = "SUBMITTED";
                    sentApplication = sentApplicationRepository.getStudentsSentApplicationFromCreatedApplication(current.getApplicationId());
                    Iterator<SentApplication> sentApplicationIterator = sentApplication.listIterator();
                    while(sentApplicationIterator.hasNext()){
                        SentApplication now = sentApplicationIterator.next();
                        if(now.getSender().getUserId() == user.getUserId()){
                            decision = now.getDecision();
                            sentApplicationId = now.getSentApplicationId();
                            break;
                        }
                    }
                }
                StudentApplicationListResponse listItem = new StudentApplicationListResponse(professorName, department, projectName, status);
                listItem.setDecision(decision);
                listItem.setSentApplicationId(sentApplicationId);
                listItem.setCreatedApplicationId(current.getApplicationId());
                listItem.setProjectDescription(current.getDescription());
                responseList.add(listItem);
            }
        } else {
            List<CreatedApplication> listCreatedApplication = createdApplicationRepository.findAll();
            Iterator<CreatedApplication> iterator = listCreatedApplication.listIterator();

            while (iterator.hasNext()) {
                CreatedApplication current = iterator.next();
                String professorName = current.getUser().getUserDetails().getFirstName() + " " + current.getUser().getUserDetails().getLastName();
                String department = current.getUser().getUserDetails().getDepartment();
                String projectName = current.getName();
                String status = "NOT SUBMITTED";
                String decision = "SUBMIT APP TO RECEIVE DECISION";
                List<SentApplication> sentApplication;
                int sentApplicationId = -1;
                if (sentCreatorIds.contains(current.getApplicationId())) {
                    status = "SUBMITTED";
                    sentApplication = sentApplicationRepository.getStudentsSentApplicationFromCreatedApplication(current.getApplicationId());
                    Iterator<SentApplication> sentApplicationIterator = sentApplication.listIterator();
                    while(sentApplicationIterator.hasNext()){
                        SentApplication now = sentApplicationIterator.next();
                        if(now.getSender().getUserId() == user.getUserId()){
                            decision = now.getDecision();
                            sentApplicationId = now.getSentApplicationId();
                            break;
                        }
                    }
                }
                StudentApplicationListResponse listItem = new StudentApplicationListResponse(professorName, department, projectName, status);
                listItem.setDecision(decision);
                listItem.setSentApplicationId(sentApplicationId);
                listItem.setCreatedApplicationId(current.getApplicationId());
                listItem.setProjectDescription(current.getDescription());
                responseList.add(listItem);
            }
        }

        return new ResponseEntity<>(responseList, HttpStatus.OK);
    }

    @GetMapping("/details")
    public Details getDetails(){
        Details response = new Details();
        response.setFirstName(getUser().getUserDetails().getFirstName());
        response.setLastName(getUser().getUserDetails().getLastName());
        response.setMajor(getUser().getUserDetails().getDepartment());
        response.setCWID(getUser().getUserDetails().getCwid());
        return response;
    }

    @PostMapping("/send-application")
    public ResponseEntity<String> sendApplication(@RequestBody ApplicationRequest applicationRequest){
        Optional<Users> senderOptional = usersRepository.findByUsername(getUser().getUsername());

        if(senderOptional.isEmpty()){
            return new ResponseEntity<>("The given sender doesn't exist.", HttpStatus.OK);
        }

        String message = applicationRequest.getMessage();
        String resumeLink = applicationRequest.getResumeLink();
        String gpa = applicationRequest.getGpa();
        String classification = applicationRequest.getClassification();
        int applicationId = applicationRequest.getApplicationId();
        Optional<CreatedApplication> createdApplicationOptional = createdApplicationRepository.findById(applicationId);
        List<Integer> sentApplications = sentApplicationRepository.getSentApplicationsCreatedApplicationId(getUser().getUserId());

        Users receiver = createdApplicationRepository.findCreatorByCreatedAppId(applicationId);

        Resume resume = null;
        if(resumeLink != null){
            resume = new Resume();
            resume.setResumeLink(resumeLink);
        }
        if(createdApplicationOptional.isEmpty()){
            return new ResponseEntity<>("Application that you were trying to send is not found", HttpStatus.OK);
        } else if(sentApplications.contains(applicationId)){
            return new ResponseEntity<>("The application that you are trying to send is already sent", HttpStatus.OK);
        }

        SentApplication application = new SentApplication(createdApplicationOptional.get(), senderOptional.get(), receiver, message, resume, gpa, classification);
        application.setDecision("IN - PROGRESS");
        sentApplicationRepository.save(application);

        return new ResponseEntity<>("Successfully sent", HttpStatus.OK);
    }

    @GetMapping("/sent-applications-list")
    public ResponseEntity<?> applicationsList() {
        Users user = getUser();
        if(user == null){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        List<SentApplication> sentApplicationsList = sentApplicationRepository.getStudentApplicationsList(user.getUserId());
        Iterator<SentApplication> iterator = sentApplicationsList.iterator();
        List<StudentApplicationListResponse> responseList = new ArrayList<>();

        while(iterator.hasNext()){
            SentApplication current = iterator.next();
            String professorName = current.getReceiver().getUserDetails().getFirstName() + " " + current.getReceiver().getUserDetails().getLastName();
            String department = current.getReceiver().getUserDetails().getDepartment();
            String projectName = current.getCreatedApplication().getName();
            String status;
            String decision;
            status = "SUBMITTED";
            decision = current.getDecision();

            StudentApplicationListResponse listItem = new StudentApplicationListResponse(current.getSentApplicationId() ,professorName, department, projectName, status);
            listItem.setDecision(decision);
            listItem.setCreatedApplicationId(current.getCreatedApplication().getApplicationId());
            responseList.add(listItem);
        }

        return new ResponseEntity<>(responseList, HttpStatus.OK);
    }

    @GetMapping("/application-details/{sent-application-id}")
    public ResponseEntity<?> getApplicationDetails(@PathVariable(value = "sent-application-id", required = true) int sentApplicationid){
        Optional<SentApplication> resultOptional = sentApplicationRepository.findById(sentApplicationid);
        if(resultOptional.isEmpty()){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        SentApplication sentApplication = resultOptional.get();
        MyUserDetails professor = sentApplication.getReceiver().getUserDetails();
        MyUserDetails student = sentApplication.getSender().getUserDetails();

        String professorName = professor.getFirstName() + " " + professor.getLastName();
        String firstName = student.getFirstName();
        String lastName = student.getLastName();
        String department = student.getDepartment();
        String cwid = student.getCwid();
        String gpa = student.getGpa();
        String classification = student.getClassification();
        String resumeLink = sentApplication.getResume().getResumeLink();
        String message = sentApplication.getMessage();
        String decision = sentApplication.getDecision();

        StudentApplicationResponse applicationResponse = new StudentApplicationResponse(professorName, firstName, lastName, department, cwid, gpa, classification, resumeLink, message, decision);
        applicationResponse.setStatusMessage("Success");
        return new ResponseEntity<>(applicationResponse, HttpStatus.OK);
    }

    @DeleteMapping("/application/{sent-application-id}")
    public ResponseEntity<String> deleteApplication(@PathVariable(value = "sent-application-id", required = true) int sentApplicationId){
        Optional<SentApplication> toDeleteOptional = sentApplicationRepository.findById(sentApplicationId);
        if(toDeleteOptional.isEmpty()){
            return new ResponseEntity<>("The sent application with the provided id was not found", HttpStatus.OK);
        }
        SentApplication toDelete = toDeleteOptional.get();
        sentApplicationRepository.delete(toDelete);
        return new ResponseEntity<>("Successfully Deleted", HttpStatus.OK);
    }

    @GetMapping("/hello")
    public String hello(){
        return "hello";
    }

    public Users getUser(){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<Users> usersOptional = usersRepository.findByUsername(username);
        return usersOptional.orElse(null);
    }
}
