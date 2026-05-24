import { useState, useEffect } from 'react'
import client from '../api/client'
import Badge from '../components/Badge'
import Modal from '../components/Modal'
import { useToast } from '../components/Toast'
import { useAuth } from '../context/AuthContext'

const BUCKETS  = ['0-30', '31-60', '61-90', '90+']
const CHANNELS = ['CALL', 'VISIT', 'INAPP', 'SMS', 'EMAIL']
const OUTCOMES = ['CONNECTED', 'NO_ANSWER', 'REFUSED']

const emptyAttempt = { loanAccountId: '', agentId: '', customerId: '', bucket: '0-30', channel: 'CALL', outcome: 'CONNECTED', notes: '' }
const emptyPolicy  = { bucket: '0-30', maxAttemptsPerDay: '', minGapMinutes: '', preferredChannels: [], doNotCall: false }
const emptyConsent = { customerId: '', channel: 'CALL', status: 'ALLOWED' }

export default function Dunning() {
  const [tab,           setTab]           = useState('attempts')
  const [attempts,      setAttempts]      = useState([])
  const [policies,      setPolicies]      = useState([])          // array of policy objects
  const [consents,      setConsents]      = useState([])
  const [consentSearch, setConsentSearch] = useState('')
  const [loading,       setLoading]       = useState(true)
  const [policyLoading, setPolicyLoading] = useState(false)
  const [consentLoading,setConsentLoading]= useState(false)
  const [attemptModal,  setAttemptModal]  = useState(false)
  const [policyModal,   setPolicyModal]   = useState(false)
  const [consentModal,  setConsentModal]  = useState(false)
  const [attemptForm,   setAttemptForm]   = useState(emptyAttempt)
  const [policyForm,    setPolicyForm]    = useState(emptyPolicy)
  const [consentForm,   setConsentForm]   = useState(emptyConsent)
  const [editingBucket, setEditingBucket] = useState(null)       // null = create, else bucket string
  const [saving,        setSaving]        = useState(false)
  const [filter,        setFilter]        = useState({ channel: '', outcome: '' })
  const toast = useToast()
  const { user } = useAuth()

  const canViewAttempts  = ['ADMIN', 'SUPERVISOR', 'COMPLIANCE'].includes(user?.role)
  const canLogAttempt    = ['ADMIN', 'SUPERVISOR', 'AGENT'].includes(user?.role)
  const canViewPolicies  = ['ADMIN', 'SUPERVISOR', 'COMPLIANCE'].includes(user?.role)
  const canEditPolicies  = user?.role === 'ADMIN'
  const canViewConsents  = ['ADMIN', 'SUPERVISOR', 'COMPLIANCE'].includes(user?.role)
  const canEditConsents  = ['ADMIN', 'SUPERVISOR'].includes(user?.role)

  // ── Load attempts ────────────────────────────────────────────────────────────
  const loadAttempts = () => {
    if (!canViewAttempts) { setLoading(false); return }
    setLoading(true)
    client.get('/dunning/attempts')
      .then(r => setAttempts(r.data || []))
      .catch(() => toast('Could not load attempts', 'error'))
      .finally(() => setLoading(false))
  }

  // ── Load policies for all 4 buckets ─────────────────────────────────────────
  const loadPolicies = async () => {
    if (!canViewPolicies) return
    setPolicyLoading(true)
    try {
      const results = await Promise.allSettled(
        BUCKETS.map(b => client.get(`/dunning/policies/${encodeURIComponent(b)}`))
      )
      const loaded = results
        .map((r, i) => r.status === 'fulfilled' ? r.value.data : null)
        .filter(Boolean)
      setPolicies(loaded)
    } catch {
      toast('Could not load contact policies', 'error')
    } finally {
      setPolicyLoading(false)
    }
  }

  // ── Search consents by customer ID ───────────────────────────────────────────
  const searchConsents = async () => {
    if (!consentSearch) return toast('Enter a Customer ID to search', 'warning')
    setConsentLoading(true)
    try {
      const r = await client.get(`/dunning/consents/${consentSearch}`)
      setConsents(r.data || [])
      if ((r.data || []).length === 0) toast('No consent records found for this customer', 'info')
    } catch {
      toast('Could not load consents', 'error')
    } finally {
      setConsentLoading(false)
    }
  }

  useEffect(() => { loadAttempts() }, [])
  useEffect(() => { if (tab === 'policies') loadPolicies() }, [tab])

  const setA = k => e => setAttemptForm(f => ({ ...f, [k]: e.target.value }))
  const setP = k => e => setPolicyForm(f => ({ ...f, [k]: e.target.value }))
  const setC = k => e => setConsentForm(f => ({ ...f, [k]: e.target.value }))

  // ── Save attempt ─────────────────────────────────────────────────────────────
  const saveAttempt = async e => {
    e.preventDefault(); setSaving(true)
    try {
      await client.post('/dunning/attempt', {
        loanAccountId: Number(attemptForm.loanAccountId),
        agentId:       Number(attemptForm.agentId),
        customerId:    Number(attemptForm.customerId),
        bucket:        attemptForm.bucket,
        channel:       attemptForm.channel,
        outcome:       attemptForm.outcome,
        notes:         attemptForm.notes || null,
      })
      toast('Contact attempt logged', 'success')
      setAttemptModal(false); setAttemptForm(emptyAttempt); loadAttempts()
    } catch (err) { toast(err.response?.data?.message || 'Failed to log attempt', 'error') }
    finally { setSaving(false) }
  }

  // ── Save / update policy ─────────────────────────────────────────────────────
  const savePolicy = async e => {
    e.preventDefault(); setSaving(true)
    try {
      const payload = {
        bucket:            policyForm.bucket,
        maxAttemptsPerDay: Number(policyForm.maxAttemptsPerDay),
        minGapMinutes:     Number(policyForm.minGapMinutes),
        preferredChannels: policyForm.preferredChannels,
        doNotCall:         policyForm.doNotCall,
      }
      if (editingBucket) {
        await client.put(`/dunning/policies/${encodeURIComponent(editingBucket)}`, payload)
        toast('Policy updated', 'success')
      } else {
        await client.post('/dunning/policies', payload)
        toast('Policy created', 'success')
      }
      setPolicyModal(false); setPolicyForm(emptyPolicy); setEditingBucket(null); loadPolicies()
    } catch (err) { toast(err.response?.data?.message || 'Failed to save policy', 'error') }
    finally { setSaving(false) }
  }

  // ── Save / update consent ────────────────────────────────────────────────────
  const saveConsent = async e => {
    e.preventDefault(); setSaving(true)
    try {
      await client.put('/dunning/consents', {
        customerId: Number(consentForm.customerId),
        channel:    consentForm.channel,
        status:     consentForm.status,
      })
      toast('Consent preference saved', 'success')
      setConsentModal(false); setConsentForm(emptyConsent)
      if (consentSearch) searchConsents()
    } catch (err) { toast(err.response?.data?.message || 'Failed to save consent', 'error') }
    finally { setSaving(false) }
  }

  const openEditPolicy = (p) => {
    setEditingBucket(p.bucket)
    setPolicyForm({
      bucket:            p.bucket,
      maxAttemptsPerDay: p.maxAttemptsPerDay || '',
      minGapMinutes:     p.minGapMinutes || '',
      preferredChannels: p.preferredChannels || [],
      doNotCall:         p.doNotCall || false,
    })
    setPolicyModal(true)
  }

  const toggleChannel = (ch) => {
    setPolicyForm(f => ({
      ...f,
      preferredChannels: f.preferredChannels.includes(ch)
        ? f.preferredChannels.filter(c => c !== ch)
        : [...f.preferredChannels, ch],
    }))
  }

  const filtered = attempts.filter(a => {
    if (filter.channel && a.channel !== filter.channel) return false
    if (filter.outcome && a.outcome !== filter.outcome) return false
    return true
  })

  const stats = {
    total:     attempts.length,
    connected: attempts.filter(a => a.outcome === 'CONNECTED').length,
    noAnswer:  attempts.filter(a => a.outcome === 'NO_ANSWER').length,
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <div className="page-title">Dunning & Contact Management</div>
          <div className="page-subtitle">Track contact attempts, configure policies and manage consent preferences</div>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          {tab === 'attempts'  && canLogAttempt   && <button className="btn btn-primary" onClick={() => setAttemptModal(true)}>+ Log Attempt</button>}
          {tab === 'policies'  && canEditPolicies  && <button className="btn btn-primary" onClick={() => { setEditingBucket(null); setPolicyForm(emptyPolicy); setPolicyModal(true) }}>+ Add Policy</button>}
          {tab === 'consents'  && canEditConsents  && <button className="btn btn-primary" onClick={() => setConsentModal(true)}>+ Set Consent</button>}
        </div>
      </div>

      {/* ── Stats ── */}
      <div className="stats-grid" style={{ marginBottom: 20 }}>
        <div className="stat-card"><div className="stat-label">Total Attempts</div><div className="stat-value">{stats.total}</div></div>
        <div className="stat-card" style={{ borderLeft: '4px solid #10b981' }}><div className="stat-label">Connected</div><div className="stat-value">{stats.connected}</div></div>
        <div className="stat-card" style={{ borderLeft: '4px solid #ef4444' }}><div className="stat-label">No Answer</div><div className="stat-value">{stats.noAnswer}</div></div>
        <div className="stat-card" style={{ borderLeft: '4px solid #6366f1' }}>
          <div className="stat-label">Connect Rate</div>
          <div className="stat-value">{stats.total ? Math.round(stats.connected / stats.total * 100) : 0}%</div>
        </div>
      </div>

      {/* ── Tabs ── */}
      <div className="tabs" style={{ marginBottom: 16 }}>
        <button className={`tab-btn ${tab === 'attempts' ? 'active' : ''}`} onClick={() => setTab('attempts')}>
          Contact Attempts ({attempts.length})
        </button>
        <button className={`tab-btn ${tab === 'policies' ? 'active' : ''}`} onClick={() => setTab('policies')}>
          Contact Policies ({policies.length})
        </button>
        <button className={`tab-btn ${tab === 'consents' ? 'active' : ''}`} onClick={() => setTab('consents')}>
          Consent Preferences
        </button>
      </div>

      {/* ══ ATTEMPTS TAB ══ */}
      {tab === 'attempts' && (
        <div className="card">
          <div className="card-header">
            <span className="card-title">Contact Attempts ({canViewAttempts ? filtered.length : 0})</span>
            <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
              <select className="form-select" style={{ width: 140, fontSize: 13 }} value={filter.channel} onChange={e => setFilter(f => ({ ...f, channel: e.target.value }))}>
                <option value="">All Channels</option>
                {CHANNELS.map(c => <option key={c}>{c}</option>)}
              </select>
              <select className="form-select" style={{ width: 150, fontSize: 13 }} value={filter.outcome} onChange={e => setFilter(f => ({ ...f, outcome: e.target.value }))}>
                <option value="">All Outcomes</option>
                {OUTCOMES.map(o => <option key={o}>{o}</option>)}
              </select>
              {canViewAttempts && <button className="btn btn-ghost btn-sm" onClick={loadAttempts}>↻</button>}
            </div>
          </div>
          {loading ? <div className="loader-wrap"><div className="spinner" /></div> : (
            <div className="table-wrapper">
              <table>
                <thead>
                  <tr><th>Attempt ID</th><th>Loan ID</th><th>Agent</th><th>Customer</th><th>Channel</th><th>Bucket</th><th>Time</th><th>Outcome</th><th>Notes</th></tr>
                </thead>
                <tbody>
                  {filtered.map(a => (
                    <tr key={a.attemptId}>
                      <td className="td-mono">{a.attemptId}</td>
                      <td className="td-primary">#L{a.loanAccountId}</td>
                      <td>Agent {a.agentId}</td>
                      <td>{a.customerId}</td>
                      <td><Badge value={a.channel} /></td>
                      <td><Badge value={a.bucket} /></td>
                      <td className="text-muted">{a.attemptTime ? new Date(a.attemptTime).toLocaleString() : '—'}</td>
                      <td><Badge value={a.outcome} /></td>
                      <td style={{ maxWidth: 180, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', fontSize: 12, color: 'var(--muted)' }}>{a.notes || '—'}</td>
                    </tr>
                  ))}
                  {filtered.length === 0 && <tr><td colSpan={9}><div className="empty-state"><p>No attempts yet</p><span>Log a contact attempt using the button above</span></div></td></tr>}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* ══ POLICIES TAB ══ */}
      {tab === 'policies' && (
        <div className="card">
          <div className="card-header">
            <span className="card-title">Contact Policies — one per DPD bucket</span>
            <button className="btn btn-ghost btn-sm" onClick={loadPolicies}>↻ Refresh</button>
          </div>
          {policyLoading ? <div className="loader-wrap"><div className="spinner" /></div> : (
            <div className="table-wrapper">
              <table>
                <thead>
                  <tr><th>Bucket</th><th>Max Attempts / Day</th><th>Min Gap (mins)</th><th>Preferred Channels</th><th>Do Not Call</th>{canEditPolicies && <th>Action</th>}</tr>
                </thead>
                <tbody>
                  {policies.map(p => (
                    <tr key={p.policyId || p.bucket}>
                      <td><Badge value={p.bucket} /></td>
                      <td style={{ fontWeight: 600 }}>{p.maxAttemptsPerDay ?? '—'}</td>
                      <td>{p.minGapMinutes ?? '—'} min</td>
                      <td>
                        {(p.preferredChannels || []).length > 0
                          ? <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
                              {p.preferredChannels.map(c => <Badge key={c} value={c} />)}
                            </div>
                          : '—'}
                      </td>
                      <td>
                        <span style={{ fontWeight: 600, color: p.doNotCall ? '#ef4444' : '#10b981' }}>
                          {p.doNotCall ? 'YES' : 'NO'}
                        </span>
                      </td>
                      {canEditPolicies && (
                        <td>
                          <button className="btn btn-outline btn-sm" style={{ padding: '3px 10px', fontSize: 12 }} onClick={() => openEditPolicy(p)}>
                            Edit
                          </button>
                        </td>
                      )}
                    </tr>
                  ))}
                  {policies.length === 0 && (
                    <tr><td colSpan={canEditPolicies ? 6 : 5}>
                      <div className="empty-state">
                        <p>No contact policies defined</p>
                        <span>Create policies for each DPD bucket (0-30, 31-60, 61-90, 90+)</span>
                      </div>
                    </td></tr>
                  )}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* ══ CONSENTS TAB ══ */}
      {tab === 'consents' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div className="card">
            <div className="card-header">
              <span className="card-title">Search Consent Preferences by Customer</span>
            </div>
            <div style={{ padding: '12px 16px', display: 'flex', gap: 8 }}>
              <input
                className="form-input"
                style={{ maxWidth: 260 }}
                type="number"
                placeholder="Enter Customer ID…"
                value={consentSearch}
                onChange={e => setConsentSearch(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && searchConsents()}
              />
              <button className="btn btn-primary" onClick={searchConsents} disabled={consentLoading}>
                {consentLoading ? 'Searching…' : 'Search'}
              </button>
            </div>
          </div>

          <div className="card">
            <div className="card-header">
              <span className="card-title">
                {consents.length > 0 ? `Consent records for Customer ${consentSearch}` : 'Consent Results'}
              </span>
            </div>
            <div className="table-wrapper">
              <table>
                <thead>
                  <tr><th>Consent ID</th><th>Customer ID</th><th>Channel</th><th>Status</th><th>Updated Date</th></tr>
                </thead>
                <tbody>
                  {consents.map(c => (
                    <tr key={c.consentId}>
                      <td className="td-mono">{c.consentId}</td>
                      <td>{c.customerId}</td>
                      <td><Badge value={c.channel} /></td>
                      <td><Badge value={c.status} /></td>
                      <td className="text-muted">{c.updatedDate || '—'}</td>
                    </tr>
                  ))}
                  {consents.length === 0 && (
                    <tr><td colSpan={5}>
                      <div className="empty-state">
                        <p>No consent records loaded</p>
                        <span>Search by Customer ID above to view their consent preferences</span>
                      </div>
                    </td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* ══ ATTEMPT MODAL ══ */}
      <Modal open={attemptModal} onClose={() => { setAttemptModal(false); setAttemptForm(emptyAttempt) }} title="Log Contact Attempt">
        <form onSubmit={saveAttempt}>
          <div className="modal-body">
            <div className="form-grid">
              <div className="form-group">
                <label className="form-label">Loan Account ID</label>
                <input className="form-input" type="number" placeholder="e.g. 1001" value={attemptForm.loanAccountId} onChange={setA('loanAccountId')} required />
              </div>
              <div className="form-group">
                <label className="form-label">Customer ID</label>
                <input className="form-input" type="number" placeholder="e.g. 500" value={attemptForm.customerId} onChange={setA('customerId')} required />
              </div>
            </div>
            <div className="form-grid">
              <div className="form-group">
                <label className="form-label">Agent ID</label>
                <input className="form-input" type="number" placeholder="e.g. 1" value={attemptForm.agentId} onChange={setA('agentId')} required />
              </div>
              <div className="form-group">
                <label className="form-label">Bucket</label>
                <select className="form-select" value={attemptForm.bucket} onChange={setA('bucket')}>
                  {BUCKETS.map(b => <option key={b}>{b}</option>)}
                </select>
              </div>
            </div>
            <div className="form-grid">
              <div className="form-group">
                <label className="form-label">Channel</label>
                <select className="form-select" value={attemptForm.channel} onChange={setA('channel')}>
                  {CHANNELS.map(c => <option key={c}>{c}</option>)}
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">Outcome</label>
                <select className="form-select" value={attemptForm.outcome} onChange={setA('outcome')}>
                  {OUTCOMES.map(o => <option key={o}>{o}</option>)}
                </select>
              </div>
            </div>
            <div className="form-group">
              <label className="form-label">Notes <span style={{ fontWeight: 400, color: 'var(--text-3)', fontSize: 11 }}>(optional)</span></label>
              <input className="form-input" placeholder="e.g. Customer requested callback tomorrow" value={attemptForm.notes} onChange={setA('notes')} />
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-outline" onClick={() => setAttemptModal(false)}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={saving}>{saving ? 'Logging…' : 'Log Attempt'}</button>
          </div>
        </form>
      </Modal>

      {/* ══ POLICY MODAL ══ */}
      <Modal open={policyModal} onClose={() => { setPolicyModal(false); setPolicyForm(emptyPolicy); setEditingBucket(null) }} title={editingBucket ? `Edit Policy — ${editingBucket} DPD` : 'Create Contact Policy'}>
        <form onSubmit={savePolicy}>
          <div className="modal-body">
            <div className="form-grid">
              <div className="form-group">
                <label className="form-label">DPD Bucket</label>
                <select className="form-select" value={policyForm.bucket} onChange={setP('bucket')} disabled={!!editingBucket}>
                  {BUCKETS.map(b => <option key={b}>{b}</option>)}
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">Max Attempts / Day</label>
                <input className="form-input" type="number" min="1" max="10" placeholder="e.g. 3" value={policyForm.maxAttemptsPerDay} onChange={setP('maxAttemptsPerDay')} required />
              </div>
            </div>
            <div className="form-group">
              <label className="form-label">Min Gap Between Attempts (minutes)</label>
              <input className="form-input" type="number" min="0" placeholder="e.g. 60" value={policyForm.minGapMinutes} onChange={setP('minGapMinutes')} required />
            </div>
            <div className="form-group">
              <label className="form-label">Preferred Channels <span style={{ fontWeight: 400, color: 'var(--text-3)', fontSize: 11 }}>(select all that apply)</span></label>
              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginTop: 6 }}>
                {CHANNELS.map(ch => (
                  <button
                    key={ch} type="button"
                    onClick={() => toggleChannel(ch)}
                    style={{
                      padding: '4px 12px', borderRadius: 6, fontSize: 13, fontWeight: 600,
                      border: '1.5px solid',
                      borderColor: policyForm.preferredChannels.includes(ch) ? 'var(--accent)' : 'var(--border)',
                      background:  policyForm.preferredChannels.includes(ch) ? 'var(--accent-bg)' : 'transparent',
                      color:       policyForm.preferredChannels.includes(ch) ? 'var(--accent)' : 'var(--text-2)',
                      cursor: 'pointer',
                    }}
                  >{ch}</button>
                ))}
              </div>
            </div>
            <div className="form-group">
              <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}>
                <input
                  type="checkbox"
                  checked={policyForm.doNotCall}
                  onChange={e => setPolicyForm(f => ({ ...f, doNotCall: e.target.checked }))}
                  style={{ width: 16, height: 16 }}
                />
                <span className="form-label" style={{ margin: 0 }}>Do Not Call flag</span>
              </label>
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-outline" onClick={() => setPolicyModal(false)}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? 'Saving…' : editingBucket ? 'Update Policy' : 'Create Policy'}
            </button>
          </div>
        </form>
      </Modal>

      {/* ══ CONSENT MODAL ══ */}
      <Modal open={consentModal} onClose={() => { setConsentModal(false); setConsentForm(emptyConsent) }} title="Set Consent Preference">
        <form onSubmit={saveConsent}>
          <div className="modal-body">
            <div className="form-group">
              <label className="form-label">Customer ID</label>
              <input className="form-input" type="number" placeholder="e.g. 500" value={consentForm.customerId} onChange={setC('customerId')} required />
            </div>
            <div className="form-grid">
              <div className="form-group">
                <label className="form-label">Channel</label>
                <select className="form-select" value={consentForm.channel} onChange={setC('channel')}>
                  {CHANNELS.map(c => <option key={c}>{c}</option>)}
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">Consent Status</label>
                <select className="form-select" value={consentForm.status} onChange={setC('status')}>
                  <option value="ALLOWED">ALLOWED — Customer consents to contact</option>
                  <option value="OPTOUT">OPTOUT — Customer has opted out</option>
                </select>
              </div>
            </div>
            <div style={{ background: 'rgba(99,102,241,.07)', border: '1px solid rgba(99,102,241,.2)', borderRadius: 8, padding: '8px 14px', fontSize: 13, color: '#6366f1' }}>
              If a consent record already exists for this customer + channel, it will be updated.
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-outline" onClick={() => setConsentModal(false)}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={saving}>{saving ? 'Saving…' : 'Save Consent'}</button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
