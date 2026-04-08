-- ============================================================
-- The Dry Fruits World — Supabase Schema + Seed Data
-- Run this in: Supabase Dashboard → SQL Editor → New query
-- ============================================================

-- 1. CATEGORIES
create table if not exists categories (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  slug text unique not null,
  description text,
  image_url text,
  is_active boolean default true,
  created_at timestamptz default now()
);

-- 2. PRODUCTS
create table if not exists products (
  id uuid primary key default gen_random_uuid(),
  category_id uuid references categories(id),
  name text not null,
  slug text unique not null,
  description text,
  short_description text,
  is_active boolean default true,
  is_bestseller boolean default false,
  rating numeric(3,2) default 0,
  review_count int default 0,
  created_at timestamptz default now()
);

-- 3. PRODUCT VARIANTS
create table if not exists product_variants (
  id uuid primary key default gen_random_uuid(),
  product_id uuid references products(id) on delete cascade,
  weight_grams int not null,
  label text not null,
  sku text unique not null,
  price numeric(10,2) not null,
  mrp numeric(10,2) not null,
  stock_qty int default 0,
  is_active boolean default true
);

-- 4. PRODUCT IMAGES
create table if not exists product_images (
  id uuid primary key default gen_random_uuid(),
  product_id uuid references products(id) on delete cascade,
  url text not null,
  alt text,
  is_primary boolean default false,
  sort_order int default 0
);

-- 5. NUTRITIONAL INFO
create table if not exists nutritional_info (
  id uuid primary key default gen_random_uuid(),
  product_id uuid unique references products(id) on delete cascade,
  serving_size text,
  calories int,
  protein_g numeric(5,2),
  carbs_g numeric(5,2),
  fat_g numeric(5,2),
  fiber_g numeric(5,2),
  sugar_g numeric(5,2)
);

-- 6. PROFILES (extends auth.users)
create table if not exists profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  full_name text,
  phone text,
  role text default 'CUSTOMER' check (role in ('CUSTOMER','MANAGER','ADMIN')),
  created_at timestamptz default now()
);

-- 7. ADDRESSES
create table if not exists addresses (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references profiles(id) on delete cascade,
  label text default 'Home',
  line1 text not null,
  line2 text,
  city text not null,
  state text not null,
  pincode text not null,
  is_default boolean default false
);

-- 8. COUPONS
create table if not exists coupons (
  id uuid primary key default gen_random_uuid(),
  code text unique not null,
  type text check (type in ('PERCENTAGE','FLAT')),
  value numeric(10,2) not null,
  min_order numeric(10,2) default 0,
  max_uses int,
  used_count int default 0,
  expires_at timestamptz,
  is_active boolean default true
);

-- 9. ORDERS
create table if not exists orders (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references profiles(id),
  status text default 'PENDING' check (status in ('PENDING','PROCESSING','PACKED','SHIPPED','DELIVERED','CANCELLED')),
  subtotal numeric(10,2) not null,
  discount numeric(10,2) default 0,
  shipping numeric(10,2) default 0,
  total numeric(10,2) not null,
  coupon_id uuid references coupons(id),
  address_snapshot jsonb,
  payment_method text check (payment_method in ('COD','ONLINE')),
  razorpay_order_id text,
  razorpay_payment_id text,
  created_at timestamptz default now()
);

-- 10. ORDER ITEMS
create table if not exists order_items (
  id uuid primary key default gen_random_uuid(),
  order_id uuid references orders(id) on delete cascade,
  variant_id uuid references product_variants(id),
  product_name text not null,
  variant_label text not null,
  qty int not null,
  unit_price numeric(10,2) not null
);

-- 11. REVIEWS
create table if not exists reviews (
  id uuid primary key default gen_random_uuid(),
  product_id uuid references products(id) on delete cascade,
  user_id uuid references profiles(id),
  rating int check (rating between 1 and 5),
  comment text,
  created_at timestamptz default now(),
  unique(product_id, user_id)
);

-- 12. BLOG POSTS
create table if not exists blog_posts (
  id uuid primary key default gen_random_uuid(),
  slug text unique not null,
  title text not null,
  excerpt text,
  content text,
  cover_image text,
  author text,
  author_role text,
  published_at timestamptz,
  read_time int,
  tags text[],
  is_published boolean default false
);

-- ============================================================
-- ROW LEVEL SECURITY
-- ============================================================
alter table profiles enable row level security;
alter table addresses enable row level security;
alter table orders enable row level security;
alter table order_items enable row level security;
alter table reviews enable row level security;

create policy "Own profile" on profiles for all using (auth.uid() = id);
create policy "Own addresses" on addresses for all using (auth.uid() = user_id);
create policy "Own orders select" on orders for select using (auth.uid() = user_id);
create policy "Own orders insert" on orders for insert with check (auth.uid() = user_id);
create policy "Public products" on products for select using (is_active = true);
create policy "Public categories" on categories for select using (is_active = true);
create policy "Public blog" on blog_posts for select using (is_published = true);

-- ============================================================
-- AUTO-CREATE PROFILE ON SIGNUP
-- ============================================================
create or replace function handle_new_user()
returns trigger language plpgsql security definer as $$
begin
  insert into profiles (id, full_name)
  values (new.id, new.raw_user_meta_data->>'full_name')
  on conflict (id) do nothing;
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function handle_new_user();

-- ============================================================
-- SEED DATA
-- ============================================================

-- Categories
insert into categories (name, slug, description, is_active) values
  ('Dry Fruits', 'dry-fruits', 'Premium almonds, cashews, walnuts, figs and more', true),
  ('Seeds', 'seeds', 'Nutrient-dense chia, flax, pumpkin seeds and more', true),
  ('Oats & Grains', 'oats-grains', 'Wholesome oats, quinoa and ancient grains', true),
  ('Gift Packs', 'gift-packs', 'Curated premium dry fruit hampers for gifting', true)
on conflict (slug) do nothing;

-- Products (using slugs matching the frontend mock-data.ts)
with cat as (select id, slug from categories)
insert into products (category_id, name, slug, short_description, is_active, is_bestseller, rating, review_count) values
  ((select id from cat where slug='dry-fruits'), 'Californian Almonds', 'californian-almonds', 'Grade A Californian almonds, raw and unsalted', true, true, 4.8, 234),
  ((select id from cat where slug='dry-fruits'), 'Premium Walnuts (Halves)', 'premium-walnuts-halves', 'Chilean walnut halves, rich in Omega-3', true, true, 4.7, 189),
  ((select id from cat where slug='dry-fruits'), 'W240 Cashews', 'w240-cashews', 'Kerala W240 grade whole cashews', true, true, 4.9, 312),
  ((select id from cat where slug='dry-fruits'), 'Premium Figs (Anjeer)', 'premium-figs-anjeer', 'Soft and naturally sweet Kashmiri figs', true, false, 4.6, 156),
  ((select id from cat where slug='dry-fruits'), 'Roasted Makhana', 'roasted-makhana', 'Lightly roasted fox nuts, zero oil', true, true, 4.7, 298),
  ((select id from cat where slug='seeds'), 'Organic Chia Seeds', 'organic-chia-seeds', 'Certified organic chia seeds, high in Omega-3', true, true, 4.8, 421),
  ((select id from cat where slug='seeds'), 'Flax Seeds', 'flax-seeds', 'Cold-pressed flax seeds, rich in lignans', true, false, 4.5, 187),
  ((select id from cat where slug='seeds'), 'Pumpkin Seeds', 'pumpkin-seeds', 'Raw pumpkin seeds, high in zinc and magnesium', true, true, 4.7, 243)
on conflict (slug) do nothing;

-- Product variants
with p as (select id, slug from products)
insert into product_variants (product_id, weight_grams, label, sku, price, mrp, stock_qty) values
  -- Californian Almonds
  ((select id from p where slug='californian-almonds'), 100, '100g', 'TDFW-ALMD-100G', 199, 249, 150),
  ((select id from p where slug='californian-almonds'), 250, '250g', 'TDFW-ALMD-250G', 450, 549, 200),
  ((select id from p where slug='californian-almonds'), 500, '500g', 'TDFW-ALMD-500G', 849, 999, 120),
  ((select id from p where slug='californian-almonds'), 1000, '1 kg', 'TDFW-ALMD-1KG', 1599, 1899, 80),
  -- Premium Walnuts
  ((select id from p where slug='premium-walnuts-halves'), 100, '100g', 'TDFW-WLNT-100G', 249, 299, 120),
  ((select id from p where slug='premium-walnuts-halves'), 250, '250g', 'TDFW-WLNT-250G', 589, 699, 110),
  ((select id from p where slug='premium-walnuts-halves'), 500, '500g', 'TDFW-WLNT-500G', 1099, 1299, 90),
  ((select id from p where slug='premium-walnuts-halves'), 1000, '1 kg', 'TDFW-WLNT-1KG', 2099, 2499, 50),
  -- W240 Cashews
  ((select id from p where slug='w240-cashews'), 100, '100g', 'TDFW-CASHW-100G', 199, 249, 200),
  ((select id from p where slug='w240-cashews'), 250, '250g', 'TDFW-CASHW-250G', 479, 579, 180),
  ((select id from p where slug='w240-cashews'), 500, '500g', 'TDFW-CASHW-500G', 899, 1099, 130),
  ((select id from p where slug='w240-cashews'), 1000, '1 kg', 'TDFW-CASHW-1KG', 1699, 1999, 70),
  -- Premium Figs
  ((select id from p where slug='premium-figs-anjeer'), 100, '100g', 'TDFW-FIG-100G', 249, 299, 100),
  ((select id from p where slug='premium-figs-anjeer'), 250, '250g', 'TDFW-FIG-250G', 620, 749, 130),
  ((select id from p where slug='premium-figs-anjeer'), 500, '500g', 'TDFW-FIG-500G', 1189, 1399, 80),
  -- Roasted Makhana
  ((select id from p where slug='roasted-makhana'), 100, '100g', 'TDFW-MKHN-100G', 210, 249, 180),
  ((select id from p where slug='roasted-makhana'), 250, '250g', 'TDFW-MKHN-250G', 499, 599, 150),
  ((select id from p where slug='roasted-makhana'), 500, '500g', 'TDFW-MKHN-500G', 949, 1099, 90),
  -- Organic Chia Seeds
  ((select id from p where slug='organic-chia-seeds'), 100, '100g', 'TDFW-CHIA-100G', 149, 179, 250),
  ((select id from p where slug='organic-chia-seeds'), 250, '250g', 'TDFW-CHIA-250G', 349, 419, 200),
  ((select id from p where slug='organic-chia-seeds'), 500, '500g', 'TDFW-CHIA-500G', 649, 779, 130),
  ((select id from p where slug='organic-chia-seeds'), 1000, '1 kg', 'TDFW-CHIA-1KG', 1249, 1499, 80),
  -- Flax Seeds
  ((select id from p where slug='flax-seeds'), 250, '250g', 'TDFW-FLAX-250G', 199, 239, 220),
  ((select id from p where slug='flax-seeds'), 500, '500g', 'TDFW-FLAX-500G', 379, 449, 150),
  ((select id from p where slug='flax-seeds'), 1000, '1 kg', 'TDFW-FLAX-1KG', 699, 849, 80),
  -- Pumpkin Seeds
  ((select id from p where slug='pumpkin-seeds'), 100, '100g', 'TDFW-PMPKN-100G', 149, 179, 180),
  ((select id from p where slug='pumpkin-seeds'), 250, '250g', 'TDFW-PMPKN-250G', 349, 419, 150),
  ((select id from p where slug='pumpkin-seeds'), 500, '500g', 'TDFW-PMPKN-500G', 649, 779, 100)
on conflict (sku) do nothing;

-- Product images (Unsplash URLs matching frontend)
with p as (select id, slug from products)
insert into product_images (product_id, url, alt, is_primary, sort_order) values
  ((select id from p where slug='californian-almonds'), 'https://images.unsplash.com/photo-1508061253366-f7da158b6d46?w=800', 'Californian Almonds', true, 0),
  ((select id from p where slug='premium-walnuts-halves'), 'https://images.unsplash.com/photo-1597362925123-77861d3fbac7?w=800', 'Premium Walnuts', true, 0),
  ((select id from p where slug='w240-cashews'), 'https://images.unsplash.com/photo-1607305387299-a3d9611cd469?w=800', 'W240 Cashews', true, 0),
  ((select id from p where slug='premium-figs-anjeer'), 'https://images.unsplash.com/photo-1601004890684-d8cbf643f5f2?w=800', 'Premium Figs', true, 0),
  ((select id from p where slug='roasted-makhana'), 'https://images.unsplash.com/photo-1515543237350-b3eea1ec8082?w=800', 'Roasted Makhana', true, 0),
  ((select id from p where slug='organic-chia-seeds'), 'https://images.unsplash.com/photo-1515543904379-3d757afe72e4?w=800', 'Organic Chia Seeds', true, 0),
  ((select id from p where slug='flax-seeds'), 'https://images.unsplash.com/photo-1610970880656-f41b15e71e1d?w=800', 'Flax Seeds', true, 0),
  ((select id from p where slug='pumpkin-seeds'), 'https://images.unsplash.com/photo-1601004890684-d8cbf643f5f2?w=800', 'Pumpkin Seeds', true, 0)
on conflict do nothing;

-- Nutritional info
with p as (select id, slug from products)
insert into nutritional_info (product_id, serving_size, calories, protein_g, carbs_g, fat_g, fiber_g, sugar_g) values
  ((select id from p where slug='californian-almonds'), '30g (23 almonds)', 173, 6.0, 6.1, 15.0, 3.5, 1.2),
  ((select id from p where slug='premium-walnuts-halves'), '30g', 196, 4.6, 4.1, 19.6, 2.0, 0.7),
  ((select id from p where slug='w240-cashews'), '30g', 163, 4.4, 9.2, 13.1, 0.9, 1.7),
  ((select id from p where slug='premium-figs-anjeer'), '40g (4 figs)', 107, 1.3, 27.6, 0.4, 4.1, 22.1),
  ((select id from p where slug='roasted-makhana'), '30g', 106, 3.6, 20.4, 0.4, 0.6, 0.0),
  ((select id from p where slug='organic-chia-seeds'), '25g (2 tbsp)', 122, 4.1, 10.6, 7.7, 9.8, 0.0),
  ((select id from p where slug='flax-seeds'), '15g (1 tbsp)', 79, 2.7, 4.3, 6.3, 3.8, 0.2),
  ((select id from p where slug='pumpkin-seeds'), '30g', 163, 8.6, 4.2, 13.9, 1.7, 0.4)
on conflict (product_id) do nothing;

-- Coupons
insert into coupons (code, type, value, min_order, max_uses, expires_at, is_active) values
  ('FIRST10', 'PERCENTAGE', 10, 299, 500, '2026-12-31 23:59:59+00', true),
  ('SAVE50', 'FLAT', 50, 499, 200, '2026-06-30 23:59:59+00', true),
  ('DIWALI25', 'PERCENTAGE', 25, 999, 500, '2025-11-05 23:59:59+00', false),
  ('BULK15', 'PERCENTAGE', 15, 1999, 100, '2026-12-31 23:59:59+00', true)
on conflict (code) do nothing;

-- Blog posts
insert into blog_posts (slug, title, excerpt, cover_image, author, author_role, published_at, read_time, tags, is_published) values
  ('top-10-health-benefits-almonds', 'Top 10 Health Benefits of Eating Almonds Daily',
   'Almonds are one of the most nutrient-dense foods on the planet. Here''s what happens to your body when you eat a small handful every day.',
   'https://images.unsplash.com/photo-1508061253366-f7da158b6d46?w=1200',
   'Ayush Nandi', 'Founder, The Dry Fruits World', '2026-03-01 00:00:00+00', 6,
   ARRAY['almonds','nutrition','health'], true),
  ('how-to-store-dry-fruits', 'How to Store Dry Fruits to Preserve Maximum Freshness',
   'Improper storage is the #1 reason dry fruits go stale or lose nutritional value. Here''s the definitive guide.',
   'https://images.unsplash.com/photo-1542838132-92c53300491e?w=1200',
   'Priya Sharma', 'Head Nutritionist', '2026-02-15 00:00:00+00', 5,
   ARRAY['storage','freshness','tips'], true),
  ('chia-seeds-complete-guide', 'Chia Seeds: The Complete Nutritional Guide for 2026',
   'This extraordinarily dense superfood deserves a deep dive. Everything you need to know about chia seeds.',
   'https://images.unsplash.com/photo-1518110925495-5fe2fda0442c?w=1200',
   'Dr. Rajan Mehta', 'Nutritionist', '2026-04-05 00:00:00+00', 7,
   ARRAY['chia-seeds','omega-3','superfoods'], false),
  ('corporate-gifting-guide-2026', 'Corporate Gifting with Premium Dry Fruits — A Guide',
   'Why dry fruits have become India''s most popular corporate gift — and how to pick the right hamper.',
   'https://images.unsplash.com/photo-1549465220-1a8b9238cd48?w=1200',
   'Ayush Nandi', 'Founder, The Dry Fruits World', '2026-01-20 00:00:00+00', 4,
   ARRAY['corporate-gifting','hampers','gifting'], true)
on conflict (slug) do nothing;
