package com.kata.backend.community;

/** A user's role within one community (independent of the platform-wide {@link com.kata.backend.user.Role}). */
public enum CommunityRole {
    MANAGER,
    /** 行政委員: keeps the community's petty cash and records what is paid out of it. */
    COMMITTEE,
    RESIDENT
}
