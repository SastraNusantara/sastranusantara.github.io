-- Sastra Nusantara Cloud Login profile table
-- Run in Supabase SQL Editor after creating a Supabase project.
create table if not exists public.sn_profiles (
  user_id uuid primary key references auth.users(id) on delete cascade,
  display_name text not null,
  role text not null check (role in ('super_admin','admin','noc','technician','cashier','staff')),
  billing_access boolean not null default false,
  mikrotik_access boolean not null default false,
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.sn_profiles enable row level security;
revoke all on table public.sn_profiles from anon, authenticated;
grant select on table public.sn_profiles to authenticated;

drop policy if exists "Users read their own Sastra profile" on public.sn_profiles;
create policy "Users read their own Sastra profile"
on public.sn_profiles for select
to authenticated
using ((select auth.uid()) = user_id);

-- No client INSERT/UPDATE/DELETE policy is created intentionally.
-- Create users in Supabase Dashboard, then insert their profile using SQL Editor.
-- Do not expose service_role/secret keys in website or APK.
