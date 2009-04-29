package com.intellij.openapi.actionSystem.ex;

import com.intellij.ide.DataManager;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.messages.MessageBus;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.List;

public class ActionUtil {
  @NonNls private static final String WAS_ENABLED_BEFORE_DUMB = "WAS_ENABLED_BEFORE_DUMB";

  private ActionUtil() {
  }

  public static void execute(@NonNls @NotNull String actionID, @NotNull InputEvent intputEvent, @Nullable Component contextComponent, @NotNull String place, int modifiers) {
    final ActionManager manager = ActionManager.getInstance();
    final AnAction action = manager.getAction(actionID);
    assert action != null : actionID;

    final DataManager dataManager = DataManager.getInstance();
    final DataContext context = contextComponent != null ? dataManager.getDataContext(contextComponent) : dataManager.getDataContext();

    final Presentation presentation = (Presentation)action.getTemplatePresentation().clone();

    final AnActionEvent event = new AnActionEvent(intputEvent, context, place, presentation, manager, modifiers);

    if (!performDumbAwareUpdate(action, event, false)) {
      return;
    }
    if (!lastUpdateAndCheckDumb(action, event, false)) {
      return;
    }

    action.actionPerformed(event);
  }

  public static void showDumbModeWarning(AnActionEvent... events) {
    MessageBus bus = null;
    List<String> actionNames = new ArrayList<String>();
    for (final AnActionEvent event : events) {
      final String s = event.getPresentation().getText();
      if (StringUtil.isNotEmpty(s)) {
        actionNames.add(s);
      }

      final Project project = (Project)event.getDataContext().getData(DataConstantsEx.PROJECT);
      if (project != null) {
        bus = project.getMessageBus();
      }
    }

    if (bus == null) {
      bus = ApplicationManager.getApplication().getMessageBus();
    }

    String message;
    final String beAvailableUntil = " be available until IntelliJ IDEA updates the indices";
    if (actionNames.isEmpty()) {
      message = "This action won't" + beAvailableUntil;
    } else if (actionNames.size() == 1) {
      message = "'" + actionNames.get(0) + "' action won't" + beAvailableUntil;
    } else {
      message = "None of the following actions will" + beAvailableUntil + ": " + StringUtil.join(actionNames, ", ");
    }

    bus.syncPublisher(Notifications.TOPIC).notify("dumb", message, "", NotificationType.INFORMATION, new NotificationListener() {
      @NotNull
      public Continue perform() {
        return Continue.REMOVE;
      }

      public Continue onRemove() {
        return Continue.REMOVE;
      }
    });
  }

  /**
   * @param action action
   * @param e action event
   * @param beforeActionPerformed whether to call
   * {@link com.intellij.openapi.actionSystem.AnAction#beforeActionPerformedUpdate(com.intellij.openapi.actionSystem.AnActionEvent)}
   * or
   * {@link com.intellij.openapi.actionSystem.AnAction#update(com.intellij.openapi.actionSystem.AnActionEvent)}
   * @return true if update tried to access indices in dumb mode
   */
  public static boolean performDumbAwareUpdate(AnAction action, AnActionEvent e, boolean beforeActionPerformed) {
    final Presentation presentation = e.getPresentation();
    final Boolean wasEnabledBefore = (Boolean)presentation.getClientProperty(WAS_ENABLED_BEFORE_DUMB);
    if (wasEnabledBefore != null) {
      presentation.putClientProperty(WAS_ENABLED_BEFORE_DUMB, null);
      presentation.setEnabled(wasEnabledBefore.booleanValue());
      presentation.setVisible(true);
    }
    final boolean enabledBeforeUpdate = presentation.isEnabled();

    final boolean notAllowed = DumbService.getInstance().isDumb() && !(action instanceof DumbAware) && !(action instanceof ActionGroup);

    try {
      if (beforeActionPerformed) {
        action.beforeActionPerformedUpdate(e);
      }
      else {
        action.update(e);
      }
    }
    catch (IndexNotReadyException e1) {
      if (notAllowed) {
        return true;
      }
      throw e1;
    }
    finally {
      if (notAllowed) {
        presentation.putClientProperty(WAS_ENABLED_BEFORE_DUMB, enabledBeforeUpdate);
        presentation.setEnabled(false);
        presentation.setVisible(false);
      }
    }
    
    return false;
  }

  public static boolean lastUpdateAndCheckDumb(AnAction action, AnActionEvent e, boolean visibilityMatters) {
    final boolean indexProblems = performDumbAwareUpdate(action, e, true);

    if (!indexProblems) {
      if (!e.getPresentation().isEnabled()) {
        return false;
      }
      if (visibilityMatters && !e.getPresentation().isVisible()) {
        return false;
      }
    }

    if (DumbService.getInstance().isDumb() && !(action instanceof DumbAware)) {
      showDumbModeWarning(e);
      return false;
    }


    return true;
  }

}