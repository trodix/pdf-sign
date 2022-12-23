package com.trodix.signature.model;

/**
 * Workflow of a sign task
 */
public enum SignTaskStatus {

    /**
     * Task as been created
     */
    IN_PROGRESS("P"),

     /**
     * Task as been signed by its recipient
     */
    SIGNED("S"),
    
    /**
     * Task as been rejected by its recipient
     */
    REJECTED("R"),

    /**
     * Task as been downloaded by its creator
     */
    DOWNLOADED("D"),


    /**
     * Task as been canceled by its creator
     */
    CANCELED("C");

    private String status;

    private SignTaskStatus(final String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return this.status;
    }
    
}
