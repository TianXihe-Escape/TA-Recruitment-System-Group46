# Design Trade-off Notes

## Purpose

This note summarizes several practical design trade-offs made in the project.

## Java desktop application

The project uses a Java desktop application because the coursework requires a stand-alone application and focuses on core software engineering principles rather than web deployment.

## JSON instead of a database

The project uses JSON-based local persistence because the coursework explicitly disallows a database. This keeps storage simple and transparent, although it also means the project needs careful local file handling.

## Modular structure

The project separates responsibilities across UI, service, repository, model, and utility layers. This improves maintainability and makes the code easier to reason about.

## Generated identifiers

The system generates record IDs instead of expecting users to create them manually. This keeps stored data more consistent and reduces input errors.

## Local demo data

The project includes local demo data so the system can be tested and demonstrated without setting up external infrastructure or manually creating every example record before each run.

## Overall balance

These trade-offs aim to keep the project aligned with coursework constraints while still producing a system that is understandable, testable, and suitable for demonstration.
