package org.hiten.blogspherebackend.enums;

public enum Role {
    SUBSCRIBER, // Free Users - read posts, comment, like
    AUTHOR, // Paid users - create posts, use AI (limited/unlimited)
    ADMIN // Full Access - manage users, moderate content
}
