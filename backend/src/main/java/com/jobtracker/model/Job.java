package com.jobtracker.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Job model. Uses Lombok @Data to generate getters, setters, equals, hashCode, toString.
 */
@Data
@NoArgsConstructor
public class Job {
    private String id;
    private String jobUrl;
    private String jobTitle;
    private String companyName;
    private String companyWebsite;
    private String location;
    private String postedAt;
    private String salary;
    private String description;
    private String numApplications;
    private String jobPostLink;
    private String applyUrl;
    private String source;
    private String atsKeywords;
    private String resumeFilename;
    private String resumePdf;
    private String dateScraped;
    private String status;
    private String notes;
    private String resumePdfUrl;
}