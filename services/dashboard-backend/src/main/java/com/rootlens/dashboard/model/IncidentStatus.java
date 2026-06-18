package com.rootlens.dashboard.model;

public enum IncidentStatus {
    OPEN, INVESTIGATING, RESOLVED;

    public boolean canTransitionTo(IncidentStatus next) {
        return switch (this) {
            case OPEN          -> next == INVESTIGATING || next == RESOLVED;
            case INVESTIGATING -> next == RESOLVED;
            case RESOLVED      -> false;
        };
    }
}
