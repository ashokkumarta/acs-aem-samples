package com.adobe.acs.samples.taskmanagement.impl;

import com.adobe.acs.samples.SampleExecutor;
import com.adobe.granite.taskmanagement.Task;
import com.adobe.granite.taskmanagement.TaskManager;
import com.adobe.granite.taskmanagement.TaskManagerException;
import com.adobe.granite.taskmanagement.TaskManagerFactory;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * Javadocs:
 * - Task Management Package: https://docs.adobe.com/docs/en/aem/6-2/develop/ref/javadoc/com/adobe/granite/taskmanagement/package-summary.html
 * - TaskManager: https://docs.adobe.com/docs/en/aem/6-2/develop/ref/javadoc/com/adobe/granite/taskmanagement/TaskManager.html
 * - Task: https://docs.adobe.com/docs/en/aem/6-2/develop/ref/javadoc/com/adobe/granite/taskmanagement/Task.html
 **/
@Component
@Service
public class SampleTaskManagementImpl implements SampleExecutor {
    private static final Logger log = LoggerFactory.getLogger(SampleTaskManagementImpl.class);

    private enum TASK_PRIORITY {
        High,
        Medium,
        Low
    }

    // ResourceResolverFactory just for illustrative purposes; you may be passing in your own ResourceResolver
    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    /**
     * Sample helper method that shows the 2 main ways to get
     *
     * @param resourceResolver the security context the TaskManager will operate under
     * @param path             the path where new Tasks will be created; if null the default Task location will be used (/etc/taskmanagement/tasks)
     * @return a TaskManager
     */
    private TaskManager getTaskManager(ResourceResolver resourceResolver, String path) {
        Resource tasksResource = resourceResolver.getResource(path);

        if (tasksResource != null) {
            // TaskManagers adapted from a Resource, will store their directs directly below this resource.
            // The most common use case is creating tasks under a project @ `<path-to-project>/jcr:content/tasks`
            return tasksResource.adaptTo(TaskManager.class);
        }

        // TaskManagers adapted from the ResourceResolver will store its tasks under /etc/taskmanagement/tasks
        // /etc/taskmanagement/tasks is OSGi configurable via com.adobe.granite.taskmanagement.impl.jcr.TaskStorageProvider along w the default archived tasks location (/etc/taskmanagement/archivedtasks)
        // These are non-project related tasks.

        // Note that a JCR Session may be used to adapt to a TaskManager as well to the same effect as the ResourceResolver method.
        return resourceResolver.adaptTo(TaskManager.class);
    }


    /**
     * Sample method that illustrates the basics of creating a Task
     */
    public void createTask() {
        ResourceResolver resourceResolver = null;

        try {
            resourceResolver = resourceResolverFactory.getServiceResourceResolver(null);

            // See the above method for getting a TaskManager
            TaskManager taskManager = getTaskManager(resourceResolver, null);

            // Create new tasks using the taskManager's TaskmanagerFactory
            TaskManagerFactory taskManagerFactory = taskManager.getTaskManagerFactory();

            // Tasks can be created using the default type
            Task task = taskManagerFactory.newTask(Task.DEFAULT_TASK_TYPE);

            task.setContentPath("/content/that/is/associated/with/this/task");
            task.setCurrentAssignee("some-users-principal-name");

            // Optionally set priority (High, Medium, Low)
            task.setProperty("taskPriority", TASK_PRIORITY.High.toString()); // or InboxItem.Priority.HIGH
            // Optionally set the start/due dates; if the dates do no exist, the task will only show in the Inbox list view and NOT the calendar view.
            task.setProperty("taskStartDate", new Date());
            task.setProperty("taskDueDate", new Date());

            // Set custom properties as well; note these will not display in the UI but can be used to make programmatic decisions
            task.setProperty("superCustomProperty", "superCustomValue");

            // Finally create the task; note this call will commit to the JCR.
            // The provided user context will be used to attempt the save, so user-permissions do come into play.
            // If no user context was provided, then a Task manager service user will be used.
            taskManager.createTask(task);

            // Do note that Tasks emit OSGi events that can be listened for and further acted upon.
            // https://docs.adobe.com/docs/en/aem/6-2/develop/ref/javadoc/com/adobe/granite/taskmanagement/TaskEvent.html#TASK_EVENT_TYPE

        } catch (TaskManagerException e) {
            log.error("Could not create task for [ {} ]", "xyz", e);
        } catch (LoginException e) {
            log.error("Could not get service user [ {} ]", "some-service-user", e);
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }
    }

    public String execute() {
        createTask();

        return "I'm a Sample Task Management code harness";
    }
}