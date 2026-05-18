# JSON Persistence Flow Notes

## Purpose

This note summarizes how data moves through the system before it is stored in local JSON files.

## High-level flow

The overall flow can be described as:

`UI -> Service -> Repository -> JSON files`

## Step-by-step explanation

- The UI collects input from TA, MO, and Admin screens.
- The service layer applies validation and business rules.
- The repository layer handles loading and saving records.
- JSON files keep the persistent application state locally.

## Why this design is important

This flow fits the coursework restriction that the system must not use a database. It also keeps the application easy to run in a classroom or demo environment without extra setup.

## Shared support behind the flow

This persistence flow depends on:

- file-path constants
- JSON parsing and formatting
- record identifier generation
- file existence checks

## Practical benefit

The design makes the system easier to demonstrate, inspect, and debug because the stored data remains local and human-readable.
