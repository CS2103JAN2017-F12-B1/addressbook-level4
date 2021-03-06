package savvytodo.model.operations;

import savvytodo.logic.commands.exceptions.CommandException;
import savvytodo.model.task.ReadOnlyTask;
import savvytodo.model.task.Task;
import savvytodo.model.task.UniqueTaskList.DuplicateTaskException;

//@@author A0124863A
/**
 * @author A0124863A
 * Undo an delete operation by adding back the deleted task
 */
public class UndoDeleteOperation extends Operation {
    private Task task;

    public static final String MESSAGE_DUPLICATE_TASK = "This task already exists in the task manager";


    public UndoDeleteOperation(ReadOnlyTask task) {
        this.task = new Task(task);
    }

    @Override
    public void execute() throws CommandException {
        assert taskManager != null;
        try {
            taskManager.addTask(task);
        } catch (DuplicateTaskException e) {
            throw new CommandException(MESSAGE_DUPLICATE_TASK);
        }
    }

    @Override
    public Operation reverse() {
        return new UndoAddOperation(task);
    }

}
