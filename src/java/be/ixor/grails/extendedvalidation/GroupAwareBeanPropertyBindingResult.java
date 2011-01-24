package be.ixor.grails.extendedvalidation;

import org.springframework.validation.BeanPropertyBindingResult;

import java.util.List;

public class GroupAwareBeanPropertyBindingResult extends BeanPropertyBindingResult {
    private List<String> groupsToCheck;
    private List<String> propertiesToCheck;
    private List<String> excludedChecks;

    public GroupAwareBeanPropertyBindingResult(Object target, String objectName) {
        super(target, objectName);
    }

    public GroupAwareBeanPropertyBindingResult(Object target, String objectName, List<String> groupsToCheck, List<String> propertiesToCheck, List<String> excludedChecks) {
        super(target, objectName);
        this.groupsToCheck = groupsToCheck;
        this.propertiesToCheck = propertiesToCheck;
        this.excludedChecks = excludedChecks;
    }

    public List<String> getGroupsToCheck() {
        return groupsToCheck;
    }

    public void setGroupsToCheck(List<String> groupsToCheck) {
        this.groupsToCheck = groupsToCheck;
    }

    public List<String> getPropertiesToCheck() {
        return propertiesToCheck;
    }

    public void setPropertiesToCheck(List<String> propertiesToCheck) {
        this.propertiesToCheck = propertiesToCheck;
    }

    public List<String> getExcludedChecks() {
        return excludedChecks;
    }

    public void setExcludedChecks(List<String> excludedChecks) {
        this.excludedChecks = excludedChecks;
    }
}
