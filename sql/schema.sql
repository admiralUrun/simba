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

    preferences varchar(256),
    notes text
);

drop table if exists addresses;
create table addresses (
    id int primary key auto_increment,
    customer_id int not null,
    city varchar(64) not null ,
    residential_complex varchar(64),
    -- Street address like вул. Володимирська 62
    address varchar(128) not null,
    entrance varchar(8),
    floor varchar(8),
    flat varchar(8),
    delivery_notes text
);
create index addresses_customer_id on addresses(customer_id);

drop table if exists ingredients;
create table ingredients (
    id int primary key,
    description varchar(256) not null,
    -- SI unit, g, mg, l, etc.
    unit varchar(4) not null,
    -- art_by mean article Belarus
    art_by int unique,
    -- see if still has an old name or was edited
    edited boolean
);

drop table if exists recipes;
create table recipes (
    id int primary key,
    name varchar(256) not null,
    type varchar(50),
    -- see if still has old name or was edited
    -- TODO: consider adding old_name instead of this field
    edited boolean
);
create index recipes_type on recipes(type);

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
    expiration_date date,
    -- TODO: change to int
    menu_type varchar(128)
);
create index offers_execution_date on offers(expiration_date);

drop table if exists offer_recipes;
create table offer_recipes (
    offer_id int,
    recipe_id int,
    -- number of servings (for 2 or 4 people)
    quantity int,
    primary key (offer_id, recipe_id)
);

drop table if exists orders;
create table orders (
    id int primary key auto_increment,
    customer_id int not null,
    address_id int not null,
    inviter_id int,
    order_day date not null,
    delivery_day date not null,
    -- deliver_from and deliver_to save time in minutes
    deliver_from int not null,
    deliver_to int not null,
    out_of_zone_delivery boolean not null,
    -- same as (dayofweek(delivery_day) == 2) :)
    delivery_on_monday boolean not null,
    total int not null,
    discount int,
    -- TODO: change to int
    payment varchar(128) not null,
    paid boolean not null,
    delivered boolean not null,
    note text
);
create index orders_customer_id on orders(customer_id);

-- TODO: consider eliminating this table in favour of adding offer_id to order_recipes
drop table if exists order_offers;
create table order_offers (
    order_id int not null,
    offer_id int not null,
    primary key (order_id, offer_id)
);

drop table if exists order_recipes;
create table order_recipes (
    order_id int not null,
    -- offer_id int not null,
    recipe_id int not null,
    quantity int
);
