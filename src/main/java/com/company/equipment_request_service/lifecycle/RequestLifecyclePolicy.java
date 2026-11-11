package com.company.equipment_request_service.lifecycle;

import com.company.equipment_request_service.entity.RequestStatus;
import com.company.equipment_request_service.web.error.InvalidStatusTransitionException;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class RequestLifecyclePolicy {

    public enum Action { SUBMIT, APPROVE, REJECT, CANCEL, EDIT }

    private static final Map<Action, RequestStatus> TARGET_STATUS;
    private static final Map<Action, Set<RequestStatus>> ALLOWED_FROM;
    private static final Map<RequestStatus, Set<Action>> ALLOWED_ACTIONS;

    static {
        Map<Action, RequestStatus> target = new EnumMap<>(Action.class);
        target.put(Action.SUBMIT, RequestStatus.UNDER_REVIEW);
        target.put(Action.APPROVE, RequestStatus.APPROVED);
        target.put(Action.REJECT, RequestStatus.REJECTED);
        target.put(Action.CANCEL, RequestStatus.CANCELLED);
        target.put(Action.EDIT, null); // редактирование статус не меняет, в changeStatus не участвует
        TARGET_STATUS = Collections.unmodifiableMap(target);

        Map<Action, Set<RequestStatus>> from = new EnumMap<>(Action.class);
        from.put(Action.SUBMIT, EnumSet.of(RequestStatus.DRAFT));
        from.put(Action.APPROVE, EnumSet.of(RequestStatus.UNDER_REVIEW));
        from.put(Action.REJECT, EnumSet.of(RequestStatus.UNDER_REVIEW));
        from.put(Action.CANCEL, EnumSet.of(RequestStatus.DRAFT, RequestStatus.UNDER_REVIEW));
        from.put(Action.EDIT, EnumSet.of(RequestStatus.DRAFT));
        ALLOWED_FROM = Collections.unmodifiableMap(from);

        Map<RequestStatus, Set<Action>> actions = new EnumMap<>(RequestStatus.class);
        actions.put(RequestStatus.DRAFT, EnumSet.of(Action.SUBMIT, Action.CANCEL, Action.EDIT));
        actions.put(RequestStatus.UNDER_REVIEW, EnumSet.of(Action.APPROVE, Action.REJECT, Action.CANCEL));
        actions.put(RequestStatus.APPROVED, EnumSet.noneOf(Action.class));
        actions.put(RequestStatus.REJECTED, EnumSet.noneOf(Action.class));
        actions.put(RequestStatus.CANCELLED, EnumSet.noneOf(Action.class));
        ALLOWED_ACTIONS = Collections.unmodifiableMap(actions);
    }

    private RequestLifecyclePolicy() {}

    public static void assertAllowed(Action action, RequestStatus current) {
        Set<RequestStatus> allowed = ALLOWED_FROM.get(action);
        if (allowed == null || !allowed.contains(current)) {
            throw new InvalidStatusTransitionException(message(action, current));
        }
    }

    public static RequestStatus targetStatus(Action action) {
        RequestStatus s = TARGET_STATUS.get(action);
        if (s == null) throw new IllegalArgumentException("Действие не меняет статус: " + action);
        return s;
    }

    public static boolean can(Action action, RequestStatus current) {
        Set<RequestStatus> allowed = ALLOWED_FROM.get(action);
        return allowed != null && allowed.contains(current);
    }

    public static Set<Action> allowedActions(RequestStatus current) {
        return Collections.unmodifiableSet(
                ALLOWED_ACTIONS.getOrDefault(current, EnumSet.noneOf(Action.class)));
    }

    private static String message(Action action, RequestStatus current) {
        return switch (action) {
            case SUBMIT  -> "Отправить на рассмотрение можно только заявку в статусе DRAFT. Текущий: " + current;
            case APPROVE -> "Одобрить можно только заявку в статусе UNDER_REVIEW. Текущий: " + current;
            case REJECT  -> "Отклонить можно только заявку в статусе UNDER_REVIEW. Текущий: " + current;
            case CANCEL  -> "Отменить можно только DRAFT или UNDER_REVIEW. Текущий: " + current;
            case EDIT    -> "Редактировать можно только заявку в статусе DRAFT. Текущий: " + current;
        };
    }
}