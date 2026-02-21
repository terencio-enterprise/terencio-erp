package es.terencio.erp.marketing.domain.model;

public enum DeliveryStatus { 
    NOT_SENT, PENDING, SENT, DELIVERED, OPENED, CLICKED, FAILED, BOUNCED, COMPLAINED;
    
    public boolean isTerminal() {
        return this == BOUNCED || this == COMPLAINED || this == FAILED;
    }

    public boolean canMarkSent() {
        return this == PENDING || this == FAILED;
    }

    public boolean canMarkDelivered() {
        return !isTerminal() && this != DELIVERED && this != OPENED && this != CLICKED;
    }

    public boolean canMarkOpened() {
        return !isTerminal() && this != PENDING && this != OPENED && this != CLICKED;
    }

    public boolean canMarkClicked() {
        return !isTerminal() && this != PENDING && this != CLICKED;
    }
}