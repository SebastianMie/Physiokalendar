// JSONTherapistDTO.java
package com.example.physiokalendar.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JSONTherapistDTO {
    private String id;
    private String name;
    private long activeSince;
    private long activeUntil;

    // Getter and Setter
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getActiveSince() {
        return activeSince;
    }

    public void setActiveSince(long activeSince) {
        this.activeSince = activeSince;
    }

    public long getActiveUntil() {
        return activeUntil;
    }

    public void setActiveUntil(long activeUntil) {
        this.activeUntil = activeUntil;
    }
}
