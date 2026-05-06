-- Create Product table
CREATE TABLE IF NOT EXISTS product (
    product_id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

-- Create Warehouse table
CREATE TABLE IF NOT EXISTS warehouse (
    id UUID PRIMARY KEY,
    address VARCHAR(500) NOT NULL
);

-- Create Order table
CREATE TABLE IF NOT EXISTS "order" (
    id UUID PRIMARY KEY,
    customer VARCHAR(255) NOT NULL,
    shipping_address VARCHAR(500) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING'
);

-- Create OrderItem table
CREATE TABLE IF NOT EXISTS order_item (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity INT NOT NULL,
    FOREIGN KEY (order_id) REFERENCES "order"(id),
    FOREIGN KEY (product_id) REFERENCES product(product_id)
);

-- Create InventoryItem table
CREATE TABLE IF NOT EXISTS inventory_item (
    id UUID PRIMARY KEY,
    warehouse_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity INT NOT NULL,
    FOREIGN KEY (warehouse_id) REFERENCES warehouse(id),
    FOREIGN KEY (product_id) REFERENCES product(product_id)
);

