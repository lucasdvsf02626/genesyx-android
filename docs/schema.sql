-- Genesyx — Supabase schema + RLS (verbatim, from Lovable extraction Answer 2 Part A).
-- NOTE: this DB has NO CHECK constraints and NO FK constraints (verified via pg_constraint).
-- Uniqueness is enforced by indexes; ownership by RLS. auth.users(id) is referenced logically only.
-- The native app reuses this exact schema/RLS; do not change column names.

-- ============================================================
-- profiles
-- ============================================================
CREATE TABLE public.profiles (
  id            uuid        PRIMARY KEY,                       -- = auth.users.id
  display_name  text,
  avatar_url    text,
  partner_id    uuid,                                          -- = other profile.id (logical only)
  created_at    timestamptz NOT NULL DEFAULT now(),
  updated_at    timestamptz NOT NULL DEFAULT now(),
  theme         text        NOT NULL DEFAULT 'dark'            -- 'light' | 'dark'
);

GRANT SELECT, INSERT, UPDATE, DELETE ON public.profiles TO authenticated;
GRANT ALL ON public.profiles TO service_role;
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

-- SELECT: own row OR the row of your linked partner
CREATE POLICY "Users view own or partner profile"
ON public.profiles FOR SELECT TO authenticated
USING (
  id = auth.uid()
  OR id = (SELECT p.partner_id FROM profiles p WHERE p.id = auth.uid())
);

-- INSERT: only your own profile row
CREATE POLICY "Users insert own profile"
ON public.profiles FOR INSERT TO authenticated
WITH CHECK (id = auth.uid());

-- UPDATE: own row, BUT partner_id MUST stay equal to its current value
-- (clients cannot change partner_id via the data API — link/unlink is service-role only)
CREATE POLICY "Users update own profile (no partner_id write)"
ON public.profiles FOR UPDATE TO authenticated
USING (id = auth.uid())
WITH CHECK (
  id = auth.uid()
  AND NOT (partner_id IS DISTINCT FROM (
    SELECT p.partner_id FROM profiles p WHERE p.id = auth.uid()
  ))
);
-- (no DELETE policy → deleteAccount uses service role)

-- ============================================================
-- cycle_settings   (unique per user)
-- ============================================================
CREATE TABLE public.cycle_settings (
  id                uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id           uuid        NOT NULL UNIQUE,
  cycle_length      integer     NOT NULL DEFAULT 28,
  period_length     integer     NOT NULL DEFAULT 5,
  last_period_date  date        NOT NULL,
  created_at        timestamptz NOT NULL DEFAULT now(),
  updated_at        timestamptz NOT NULL DEFAULT now()
);

GRANT SELECT, INSERT, UPDATE, DELETE ON public.cycle_settings TO authenticated;
GRANT ALL ON public.cycle_settings TO service_role;
ALTER TABLE public.cycle_settings ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users view own cycle_settings"   ON public.cycle_settings FOR SELECT TO authenticated USING (auth.uid() = user_id);
CREATE POLICY "Users insert own cycle_settings" ON public.cycle_settings FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users update own cycle_settings" ON public.cycle_settings FOR UPDATE TO authenticated USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users delete own cycle_settings" ON public.cycle_settings FOR DELETE TO authenticated USING (auth.uid() = user_id);

-- ============================================================
-- daily_logs   (unique per (user, date)) — idx_daily_logs_user_date (user_id, date DESC) powers getStreak
-- ============================================================
CREATE TABLE public.daily_logs (
  id             uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id        uuid        NOT NULL,
  date           date        NOT NULL,
  mood           text,
  energy         text,                              -- 'low' | 'normal' | 'high' (app-enforced)
  symptoms       text[]      NOT NULL DEFAULT '{}',
  sleep_minutes  integer,
  water_ml       integer     NOT NULL DEFAULT 0,
  supplements    text[]      NOT NULL DEFAULT '{}',
  notes          text,
  created_at     timestamptz NOT NULL DEFAULT now(),
  updated_at     timestamptz NOT NULL DEFAULT now(),
  UNIQUE (user_id, date)
);

GRANT SELECT, INSERT, UPDATE, DELETE ON public.daily_logs TO authenticated;
GRANT ALL ON public.daily_logs TO service_role;
ALTER TABLE public.daily_logs ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users view own daily_logs"   ON public.daily_logs FOR SELECT TO authenticated USING (auth.uid() = user_id);
CREATE POLICY "Users insert own daily_logs" ON public.daily_logs FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users update own daily_logs" ON public.daily_logs FOR UPDATE TO authenticated USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users delete own daily_logs" ON public.daily_logs FOR DELETE TO authenticated USING (auth.uid() = user_id);

-- ============================================================
-- ph_readings   — ph_readings_user_recorded_idx (user_id, recorded_at DESC)
-- ============================================================
CREATE TABLE public.ph_readings (
  id           uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id      uuid        NOT NULL,
  ph_value     numeric     NOT NULL,                 -- app rounds to 1 decimal, range 4.5–9.0
  recorded_at  timestamptz NOT NULL DEFAULT now(),
  notes        text,
  created_at   timestamptz NOT NULL DEFAULT now(),
  updated_at   timestamptz NOT NULL DEFAULT now()
);

GRANT SELECT, INSERT, UPDATE, DELETE ON public.ph_readings TO authenticated;
GRANT ALL ON public.ph_readings TO service_role;
ALTER TABLE public.ph_readings ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users select own ph readings" ON public.ph_readings FOR SELECT TO authenticated USING (auth.uid() = user_id);
CREATE POLICY "Users insert own ph readings" ON public.ph_readings FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users update own ph readings" ON public.ph_readings FOR UPDATE TO authenticated USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users delete own ph readings" ON public.ph_readings FOR DELETE TO authenticated USING (auth.uid() = user_id);

-- ============================================================
-- partner_invites — UNIQUE(code), inviter_idx(inviter_id), email_idx(lower(invitee_email))
-- ============================================================
CREATE TABLE public.partner_invites (
  id             uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
  inviter_id     uuid        NOT NULL,
  invitee_email  text        NOT NULL,
  code           text        NOT NULL UNIQUE,                  -- 16-char hex
  status         text        NOT NULL DEFAULT 'pending',       -- 'pending' | 'accepted' | 'revoked'
  expires_at     timestamptz NOT NULL DEFAULT (now() + interval '14 days'),
  accepted_by    uuid,
  accepted_at    timestamptz,
  created_at     timestamptz NOT NULL DEFAULT now()
);

GRANT SELECT, INSERT, UPDATE, DELETE ON public.partner_invites TO authenticated;
GRANT ALL ON public.partner_invites TO service_role;
ALTER TABLE public.partner_invites ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Inviter sees own invites"
ON public.partner_invites FOR SELECT TO authenticated
USING (inviter_id = auth.uid());

CREATE POLICY "Invitee sees invites by email"
ON public.partner_invites FOR SELECT TO authenticated
USING (
  lower(invitee_email) = lower(auth.jwt() ->> 'email')
  AND COALESCE((auth.jwt() ->> 'email_verified')::boolean, false) = true
);

CREATE POLICY "Inviter creates invites"
ON public.partner_invites FOR INSERT TO authenticated
WITH CHECK (inviter_id = auth.uid());

-- UPDATE locked to revocations only; 'accepted' transition is service-role only
CREATE POLICY "Inviter revokes own invites (strict)"
ON public.partner_invites FOR UPDATE TO authenticated
USING (inviter_id = auth.uid())
WITH CHECK (inviter_id = auth.uid() AND status = 'revoked');
-- (no DELETE policy)

-- ============================================================
-- Trigger: auto-create profile on signup (attached to auth.users by Lovable managed setup)
-- ============================================================
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  INSERT INTO public.profiles (id, display_name, avatar_url)
  VALUES (
    NEW.id,
    COALESCE(NEW.raw_user_meta_data->>'display_name',
             NEW.raw_user_meta_data->>'full_name',
             split_part(NEW.email, '@', 1)),
    NEW.raw_user_meta_data->>'avatar_url'
  )
  ON CONFLICT (id) DO NOTHING;
  RETURN NEW;
END $$;

-- Generic touch trigger (defined but NOT attached to any table)
CREATE OR REPLACE FUNCTION public.touch_updated_at()
RETURNS trigger LANGUAGE plpgsql SET search_path = public AS $$
BEGIN NEW.updated_at = now(); RETURN NEW; END $$;
