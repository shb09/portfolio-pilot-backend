package com.portfoliopilot.entity;

/** Append-only event types. No sensitive data is ever recorded. */
public enum EventType {
    PORTFOLIO_VIEW,
    PROJECT_CLICK,
    GITHUB_CLICK,
    RESUME_CLICK,
    LINKEDIN_CLICK
}
