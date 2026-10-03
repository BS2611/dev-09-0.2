# Name of application: My Goal Tracker

# Who did What

# Version 0.4
1. Keerat Kaur: Implemented permanent saving of Goal data (DAL interface, FileSystemDAL, data/goals.txt) and the success message on the Goal page
2. Sujata Giri: Implemented the Stored Goals page (JavaFX TableView sorted by creation date, most recent on top) with Back/Refresh buttons and navigation from the Home page
3. Bhavpreet Singh: Cleaned the project folder (removed non-source files), updated the ReadMe.md, and created the final ZIP

# Version 0.2
1. Sujata Giri: Created the Goal and Home Page UI, implemented page navigation and integrated Goal creation with the backend
2. Bhavpreet Singh: Created the backend classes, validation, and data storage
3. Keerat Kaur: Reviewed final integration, removed non-code files and final submission

# Technical-Spec
1. Keerat Kaur: Data Model, Objective, and References
2. Sujata Giri: Class Diagram
3. Bhavpreet Singh: Sequence Diagram

# Functional-Spec
1. Keerat Kaur: Mockup#1
2. Sujata Giri: Use Case, Objective, References, Problem Statement, Scope
3. Bhavpreet Singh: Mockup#2, functional requirements, non-functional requirements


# Any other instruction that users need to know:
Run the project with Zulu 23 using Maven.

Please run 'Main.java' (package cs151.application) to run the application.

Data note: Goals are saved permanently in the file data/goals.txt, which is created automatically at runtime. No manual setup is required. The sqlite-jdbc library used by the backend is declared as a dependency in pom.xml.

Thank You!
