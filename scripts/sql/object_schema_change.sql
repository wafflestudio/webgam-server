alter table page_object
add column background_color varchar(255) null,
add column is_reversed      bit          null,
add column letter_spacing   int          null,
add column line_height      int          null,
add column rotate_degree    int          null,
add column stroke_color     varchar(255) null,
add column stroke_width     int          null,
add column opacity          int          null;
