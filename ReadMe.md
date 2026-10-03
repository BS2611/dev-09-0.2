# Name of application: My Goal Tracker
#Version: 0.4

# who did what:
1. Sujata Giri: Created the Goal and Home Page UI, implemented page navigation and integrated Goal creation with the backend 
2. Bhavpreet Singh: Created the backend classes, validation, and data storage.
3. Keerat Kaur: reviewed final integration, removed non code files and final submission

# Version 0.4 Changes

- Added the 'DAL' interface for goal persistence.
- Integrated 'FileSystemDAL' into the Maven source structure.
- Added permanent Goal storage using 'data/goals.txt'.
- Added loading of stored Goals from the file system.
- Added a Stored Goals page using JavaFX 'TableView'.
- Added columns for name, description, category, target date, status, progress, and creation date.
- Sorted Goals by creation date in descending order.
- Added Create Goal and View Goals navigation.
- Added Back and Refresh buttons to the Stored Goals page.
- Removed duplicate Java files from the project root.
- Ignored runtime data files through '.gitignore'.
- Tested goal creation, permanent storage, application restart, goal listing, navigation, and Maven tests.


# Functional-Spec
1. Keerat Kaur: Mockup#1
2. Sujata Giri: Use Case, Objective, References, Problem Statement, Scope
3. Bhavpreet Singh: Mockup#2, functional requirements, non-functional requirements


# Any other instruction that users need to know:
Run the project with Zulu 23 using Maven.

Please run 'Main.java' to run the application

Backend note: data is stored in a SQLite database (data/my-goal-tracker.db) that is created automatically at runtime. No manual database setup is required.

Thank You!
