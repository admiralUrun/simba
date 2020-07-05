drop table if exists customers;
create table customers (
    id int primary key auto_increment,
    first_name varchar(64) not null,
    last_name varchar(64),

    phone varchar(16) not null,
    phone_note varchar(64),
    phone2 varchar(16),
    phone2_note varchar(64),

    instagram varchar(64) unique,

    preferences varchar(255),
    notes text
);

drop table if exists addresses;
create table addresses (
    id int primary key auto_increment,
    customer_id int not null,
    city varchar(64) not null ,
    residential_complex varchar(64),
    address varchar(128) not null ,
    -- Street address like вул. Володимирська 62
    entrance varchar(8),
    floor varchar(8),
    flat varchar(8),
    note_for_courier text
);

drop table if exists ingredients;
create table ingredients (
    id int primary key auto_increment,
    description varchar(256) not null,
    -- SI unit, g, mg, l, etc.
    unit varchar(4) not null,
    art_by int unique
    -- art_by mean article Belarus
);

drop table if exists recipes;
create table recipes (
    id int primary key auto_increment,
    name varchar(256) not null
);

drop table if exists recipe_ingredients;
create table recipe_ingredients (
    recipe_id int,
    ingredient_id int,
    netto decimal,
    primary key (recipe_id, ingredient_id)
);

drop table if exists offers;
create table offers (
    id int primary key auto_increment,
    name varchar(128),
    price int not null,
    execution_date date,
    menu_type varchar(128)
);

drop table if exists offer_recipes;
create table offer_recipes (
    offer_id int,
    recipe_id int,
    quantity int,

    primary key (offer_id, recipe_id)
);

drop table if exists orders;
create table orders (
    id int primary key auto_increment,
    customer_id int not null,
    address_id int not null,
    order_day date not null,
    delivery_day date not null,
    deliver_from time not null,
    deliver_to time not null,

    total int not null,

    offline_delivery boolean not null,
    delivery_on_monday boolean not null,
    paid boolean not null,
    delivered boolean not null,
    note text
);

drop table if exists order_offers;
create table order_offers (
    order_id int not null,
    offer_id int not null,
    primary key (order_id, offer_id)
);

drop table if exists order_recipes;
create table order_recipes (
    order_id int not null,
    recipe_id int not null,
    quantity int
);
