CREATE TABLE partner_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    partner_id UUID NOT NULL,
    level VARCHAR(20) NOT NULL,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    assigned_by UUID REFERENCES users(id) ON DELETE SET NULL,
    UNIQUE (user_id, partner_id)
);

CREATE INDEX idx_partner_members_user ON partner_members(user_id);
CREATE INDEX idx_partner_members_partner ON partner_members(partner_id);
