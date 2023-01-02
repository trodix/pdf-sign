package com.trodix.signature.domain.model;

/**
 * Workflow of a sign task
 */
public enum SignTaskStatus {

    /**
     * Task as been created
     */
    IN_PROGRESS("P"),

     /**
     * Task files have been signed by all the recipients
     */
    SIGNED("S"),
    
    /**
     * Task have been rejected by its recipient
     */
    REJECTED("R"),

    /**
     * Task have been downloaded by its creator
     */
    DOWNLOADED("D"),


    /**
     * Task have been canceled by its creator
     */
    CANCELED("C");

    private String status;

    private SignTaskStatus(final String status) {
        this.status = status;
    }

    public static SignTaskStatus fromValue(final String status){
        for(SignTaskStatus e : SignTaskStatus.values()){
            if(e.status.equals(status)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + status);
    }

    @Override
    public String toString() {
        return this.status;
    }
    
}
