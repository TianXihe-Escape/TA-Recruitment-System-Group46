# Validation Rules Notes

## Purpose

This note summarizes the main validation rules used in the TA Recruitment System.

## Why validation matters

Because the system stores persistent data in local JSON files, validation is important for keeping stored records consistent and meaningful before they are saved.

## Applicant profile validation

Typical checks include:

- name must not be blank
- email must be in a valid format
- phone number must match the accepted format

## Academic profile validation

Typical checks include:

- programme must not be blank
- year of study must not be blank

## Skill validation

Typical checks include:

- duplicate skills should be rejected
- duplicate checks should ignore case
- skill counts should remain within a reasonable limit
- each skill item should stay within a reasonable length

## Job-posting validation

Typical checks include:

- module code must not be blank
- module title must not be blank
- duties must not be blank
- required skills must be present
- application deadline must be valid

## Why this helps

These checks improve data quality and reduce the chance that invalid information will be written into local storage.
