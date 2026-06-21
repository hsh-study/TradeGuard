create table news_articles (
    id bigint not null auto_increment primary key,
    provider varchar(30) not null,
    title varchar(500) not null,
    summary varchar(1500) not null,
    origin_link varchar(1000),
    link varchar(1000) not null,
    publisher varchar(255),
    published_at timestamp null,
    collected_at timestamp not null,
    query_text varchar(100) not null,
    normalized_title_hash varchar(64) not null,
    source_hash varchar(64) not null,
    category varchar(40) not null,
    sentiment varchar(20) not null,
    importance varchar(20) not null,
    short_reason varchar(500) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint uk_news_articles_source_hash unique (source_hash)
);
create index idx_news_articles_collected_at on news_articles(collected_at);
create index idx_news_articles_importance on news_articles(importance, collected_at);
create index idx_news_articles_title_hash on news_articles(normalized_title_hash);

create table news_stock_mentions (
    id bigint not null auto_increment primary key,
    news_article_id bigint not null,
    stock_code varchar(12) not null,
    stock_name varchar(100) not null,
    match_type varchar(30) not null,
    confidence decimal(6,4) not null,
    created_at timestamp not null,
    constraint fk_news_mentions_article foreign key (news_article_id) references news_articles(id),
    constraint uk_news_mentions_article_stock unique (news_article_id, stock_code)
);
create index idx_news_mentions_stock on news_stock_mentions(stock_code);

create table news_import_histories (
    id bigint not null auto_increment primary key,
    query_text varchar(100) not null,
    requested_display int not null,
    fetched_count int not null,
    saved_count int not null,
    duplicated_count int not null,
    status varchar(20) not null,
    failure_reason varchar(500),
    started_at timestamp not null,
    finished_at timestamp not null
);
create index idx_news_import_histories_query on news_import_histories(query_text, started_at);
