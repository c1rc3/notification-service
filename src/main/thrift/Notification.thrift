#@namespace scala circe.cpp.notification
struct TNotification {
    1: required string id
    2: required string sender
    3: required string receiver
    4: required string notifyType
    5: required string data
    6: required bool isRead
    7: required i64 createdTime
    8: required i64 updatedTime
    9: required i64 readTime
}

struct TListNotificationResponse {
    1: list<TNotification> contents
    2: i64 totalElement
    3: i32 totalPage
    4: i32 currentPage
}

service TNotificationService {
    string addNotification(
        1: string sender,
        2: string receiver,
        3: string notifyType,
        4: string data
    )

    bool markRead(
        2: string notificationId
    )

    bool markUnread(
        1: string receiver,
        2: string notificationId
    )

    i32 markReadAll(
        1: string receiver
    )

    TListNotificationResponse getNotifications(
        1: string receiver
        2: i32 page = 1
        3: i32 size = 10
        4: optional string notifyType
        5: optional list<string> sorts
    )

    TListNotificationResponse getUnRead(
        1: string receiver
        2: i32 page = 1
        3: i32 size = 10
        4: optional string notifyType
        5: optional list<string> sorts
    )

    i64 numUnread(
        1: string receiver,
        2: optional string notifyType
    )
}