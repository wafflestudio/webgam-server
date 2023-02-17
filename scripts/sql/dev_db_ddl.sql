create table user
(
    id          bigint auto_increment
        primary key,
    created_at  datetime(6)  null,
    created_by  varchar(255) null,
    is_deleted  bit          not null,
    modified_at datetime(6)  null,
    modified_by varchar(255) null,
    email       varchar(255) null,
    password    varchar(255) null,
    user_id     varchar(255) null,
    username    varchar(255) null,
    constraint UK_a3imlf41l37utmxiquukk8ajc
        unique (user_id),
    constraint UK_ob8kqyqqgmefl0aco34akdtpe
        unique (email)
);

create table project
(
    id          bigint auto_increment
        primary key,
    created_at  datetime(6)  null,
    created_by  varchar(255) null,
    is_deleted  bit          not null,
    modified_at datetime(6)  null,
    modified_by varchar(255) null,
    title       varchar(255) null,
    variables   json         null,
    owner_id    bigint       null,
    constraint FK9ydhxbq67a3m0ek560r2fq38g
        foreign key (owner_id) references user (id)
);

create table project_page
(
    id          bigint auto_increment
        primary key,
    created_at  datetime(6)  null,
    created_by  varchar(255) null,
    is_deleted  bit          not null,
    modified_at datetime(6)  null,
    modified_by varchar(255) null,
    name        varchar(255) null,
    project_id  bigint       null,
    constraint FKhu73es319466ogs83w8yuo5m5
        foreign key (project_id) references project (id)
);

create table object_event
(
    id              bigint auto_increment
        primary key,
    created_at      datetime(6)  null,
    created_by      varchar(255) null,
    is_deleted      bit          not null,
    modified_at     datetime(6)  null,
    modified_by     varchar(255) null,
    transition_type varchar(255) null,
    next_page_id    bigint       null,
    object_id       bigint       null,
    constraint FK53fh2eghq9y465qjhfadvrqa8
        foreign key (next_page_id) references project_page (id)
);

create table page_object
(
    id           bigint auto_increment
        primary key,
    created_at   datetime(6)  null,
    created_by   varchar(255) null,
    is_deleted   bit          not null,
    modified_at  datetime(6)  null,
    modified_by  varchar(255) null,
    font_size    int          null,
    height       int          not null,
    image_source varchar(255) null,
    name         varchar(255) null,
    text_content varchar(255) null,
    type         varchar(255) null,
    width        int          not null,
    x_position   int          not null,
    y_position   int          not null,
    z_index      int          not null,
    event_id     bigint       null,
    page_id      bigint       null,
    constraint FK3wyywkgf0s7df0v3ei2wnnlxp
        foreign key (page_id) references project_page (id),
    constraint FKka8nxl5nemn0pgj4segx6g6ae
        foreign key (event_id) references object_event (id)
);

alter table object_event
    add constraint FKk0w8qfv27kr2hapdy59v6qf9b
        foreign key (object_id) references page_object (id);

