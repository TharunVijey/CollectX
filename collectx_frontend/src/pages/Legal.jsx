import { useState, useEffect } from 'react'
import client from '../api/client'
import Badge from '../components/Badge'
import Modal from '../components/Modal'
import { useToast } from '../components/Toast'
import { useAuth } from '../context/AuthContext'

const emptyAction  = { loanAccountId: '', customerId: '', actionType: 'NOTICE', caseNumber: '', courtName: '', filedDate: '', notes: '', status: 'OPEN' }
const emptyWriteoff = { loanAccountId: '', customerId: '', writeOffAmount: '', reason: '', approvedBy: '', status: 'POSTED' }
const emptyRecovery = { loanAccountId: '', customerId: '', recoveredAmount: '', recoveryDate: '', recoveryType: 'CASH', notes: '', status: 'PENDING' }

export default function Legal() {
  const [tab,         setTab]        = useState('actions')
  const [actions,     setActions]    = useState([])
  const [writeoffs,   setWriteoffs]  = useState([])
  const [recoveries,  setRecoveries] = useState([])
  const [loading,     setLoading]    = useState(true)
  const [actionModal, setActionModal]= useState(false)
  const [woModal,     setWoModal]    = useState(false)
  const [recModal,    setRecModal]   = useState(false)
  const [actionForm,  setActionForm] = useState(emptyAction)
  const [woForm,      setWoForm]     = useState(emptyWriteoff)
  const [recForm,     setRecForm]    = useState(emptyRecovery)
  const [saving,      setSaving]     = useState(false)
  const toast = useToast()
  const { user } = useAuth()
  const canFileAction    = ['ADMIN', 'SUPERVISOR', 'COMPLIANCE'].includes(user?.role)
  const canWriteOff      = ['ADMIN', 'COMPLIANCE'].includes(user?.role)
  const canRecordRecovery = ['ADMIN', 'RECOVERY', 'COMPLIANCE'].includes(user?.role)
  const canEditStatus    = ['ADMIN', 'COMPLIANCE', 'RECOVERY'].includes(user?.role)

  const ACTION_STATUSES  = ['OPEN', 'NOTICE_SENT', 'HEARING', 'DECREE', 'DISPOSED']
  const WRITEOFF_STATUSES = ['DRAFT', 'PENDING', 'POSTED', 'REVERSED']
  const RECOVERY_STATUSES = ['PENDING', 'COMPLETED', 'FAILED']

  const updateStatus = async (type, id, newStatus) => {
    try {
      const url = `/legal/${type}/${id}/status?status=${newStatus}`
      const res = await client.patch(url)
      if (type === 'action')   setActions(prev => prev.map(a => a.legalActionId === id ? res.data : a))
      if (type === 'writeoff') setWriteoffs(prev => prev.map(w => w.writeOffId === id ? res.data : w))
      if (type === 'recovery') setRecoveries(prev => prev.map(r => r.recoveryId === id ? res.data : r))
      toast('Status updated', 'success')
    } catch (err) { toast(err.response?.data?.message || 'Update failed', 'error') }
  }

  const loadAll = () => {
    setLoading(true)
    Promise.allSettled([
      client.get('/legal/actions'),
      client.get('/legal/writeoffs'),
      client.get('/legal/recoveries'),
    ]).then(([a, w, r]) => {
      setActions(a.status === 'fulfilled' ? a.value.data || [] : [])
      setWriteoffs(w.status === 'fulfilled' ? w.value.data || [] : [])
      setRecoveries(r.status === 'fulfilled' ? r.value.data || [] : [])
    }).finally(() => setLoading(false))
  }

  useEffect(loadAll, [])

  const set = setter => k => e => setter(f => ({ ...f, [k]: e.target.value }))

  const saveAction = async e => {
    e.preventDefault(); setSaving(true)
    try {
      const res = await client.post('/legal/action', {
        loanAccountId: Number(actionForm.loanAccountId),
        customerId: Number(actionForm.customerId),
        actionType: actionForm.actionType,
        caseNumber: actionForm.caseNumber,
        courtName: actionForm.courtName,
        filedDate: actionForm.filedDate,
        notes: actionForm.notes,
        status: actionForm.status,
      })
      toast('Legal action filed', 'success')
      // Optimistically add the new action to the list immediately, then do a full refresh
      if (res.data) setActions(prev => [...prev, res.data])
      setActionModal(false); setActionForm(emptyAction)
      // Full refresh after a short delay to ensure DB commit is visible
      setTimeout(loadAll, 300)
    } catch (err) { toast(err.response?.data?.message || 'Failed to file action', 'error') }
    finally { setSaving(false) }
  }

  const saveWriteoff = async e => {
    e.preventDefault(); setSaving(true)
    try {
      const res = await client.post('/legal/writeoff', {
        loanAccountId: Number(woForm.loanAccountId),
        customerId: Number(woForm.customerId),
        writeOffAmount: Number(woForm.writeOffAmount),
        reason: woForm.reason,
        approvedBy: woForm.approvedBy,
        status: woForm.status,
      })
      toast('Write-off recorded', 'success')
      if (res.data) setWriteoffs(prev => [...prev, res.data])
      setWoModal(false); setWoForm(emptyWriteoff)
      setTimeout(loadAll, 300)
    } catch (err) { toast(err.response?.data?.message || 'Failed to record write-off', 'error') }
    finally { setSaving(false) }
  }

  const saveRecovery = async e => {
    e.preventDefault(); setSaving(true)
    try {
      const res = await client.post('/legal/recovery', {
        loanAccountId: Number(recForm.loanAccountId),
        customerId: Number(recForm.customerId),
        recoveredAmount: Number(recForm.recoveredAmount),
        recoveryDate: recForm.recoveryDate,
        recoveryType: recForm.recoveryType,
        notes: recForm.notes,
        status: recForm.status,
      })
      toast('Recovery recorded', 'success')
      if (res.data) setRecoveries(prev => [...prev, res.data])
      setRecModal(false); setRecForm(emptyRecovery)
      setTimeout(loadAll, 300)
    } catch (err) { toast(err.response?.data?.message || 'Failed to record recovery', 'error') }
    finally { setSaving(false) }
  }

  const totalWrittenOff = writeoffs.reduce((s, w) => s + (w.writeOffAmount || 0), 0)
  const totalRecovered  = recoveries.reduce((s, r) => s + (r.recoveredAmount || 0), 0)

  return (
    <div>
      <div className="page-header">
        <div>
          <div className="page-title">Legal & Compliance</div>
          <div className="page-subtitle">Legal actions, write-offs and post-write-off recovery</div>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          {tab === 'actions' && canFileAction && (
            <button className="btn btn-primary" onClick={() => setActionModal(true)}>+ File Action</button>
          )}
          {tab === 'writeoffs' && canWriteOff && (
            <button className="btn btn-primary" onClick={() => setWoModal(true)}>+ Write-Off</button>
          )}
          {tab === 'recoveries' && canRecordRecovery && (
            <button className="btn btn-primary" onClick={() => setRecModal(true)}>+ Record Recovery</button>
          )}
        </div>
      </div>

      <div className="stats-grid" style={{ marginBottom: 20 }}>
        <div className="stat-card">
          <div className="stat-label">Legal Actions</div>
          <div className="stat-value">{actions.length}</div>
        </div>
        <div className="stat-card" style={{ borderLeft: '4px solid #ef4444' }}>
          <div className="stat-label">Total Written Off</div>
          <div className="stat-value">₹{totalWrittenOff.toLocaleString()}</div>
        </div>
        <div className="stat-card" style={{ borderLeft: '4px solid #10b981' }}>
          <div className="stat-label">Total Recovered</div>
          <div className="stat-value">₹{totalRecovered.toLocaleString()}</div>
        </div>
        <div className="stat-card" style={{ borderLeft: '4px solid #6366f1' }}>
          <div className="stat-label">Recovery Rate</div>
          <div className="stat-value">
            {totalWrittenOff > 0 ? Math.round(totalRecovered / totalWrittenOff * 100) : 0}%
          </div>
        </div>
      </div>

      <div className="tabs">
        <button className={`tab-btn ${tab === 'actions' ? 'active' : ''}`} onClick={() => setTab('actions')}>Legal Actions ({actions.length})</button>
        <button className={`tab-btn ${tab === 'writeoffs' ? 'active' : ''}`} onClick={() => setTab('writeoffs')}>Write-Offs ({writeoffs.length})</button>
        <button className={`tab-btn ${tab === 'recoveries' ? 'active' : ''}`} onClick={() => setTab('recoveries')}>Recovery ({recoveries.length})</button>
      </div>

      <div className="card">
        <div className="card-header">
          <span className="card-title">
            {tab === 'actions' ? 'Legal Actions' : tab === 'writeoffs' ? 'Write-Off Records' : 'Post Write-Off Recoveries'}
          </span>
          <button className="btn btn-ghost btn-sm" onClick={loadAll}>↻ Refresh</button>
        </div>

        {loading ? <div className="loader-wrap"><div className="spinner" /></div> : (
          <div className="table-wrapper">
            {tab === 'actions' && (
              <table>
                <thead>
                  <tr><th>Action ID</th><th>Loan ID</th><th>Customer</th><th>Type</th><th>Case Number</th><th>Court</th><th>Filed Date</th><th>Status</th></tr>
                </thead>
                <tbody>
                  {actions.map(a => (
                    <tr key={a.legalActionId}>
                      <td className="td-mono">{a.legalActionId}</td>
                      <td className="td-primary">#L{a.loanAccountId}</td>
                      <td>{a.customerId}</td>
                      <td><Badge value={a.actionType} /></td>
                      <td className="td-mono" style={{ fontSize: 12 }}>{a.caseNumber || '—'}</td>
                      <td>{a.courtName || '—'}</td>
                      <td className="text-muted">{a.filedDate || '—'}</td>
                      <td><Badge value={a.status || 'OPEN'} /></td>
                    </tr>
                  ))}
                  {actions.length === 0 && <tr><td colSpan={8}><div className="empty-state"><p>No legal actions filed</p></div></td></tr>}
                </tbody>
              </table>
            )}

            {tab === 'writeoffs' && (
              <table>
                <thead>
                  <tr><th>Write-Off ID</th><th>Loan ID</th><th>Customer</th><th>Amount</th><th>Reason</th><th>Approved By</th><th>Status</th></tr>
                </thead>
                <tbody>
                  {writeoffs.map(w => (
                    <tr key={w.writeOffId}>
                      <td className="td-mono">{w.writeOffId}</td>
                      <td className="td-primary">#L{w.loanAccountId}</td>
                      <td>{w.customerId}</td>
                      <td style={{ fontWeight: 600, color: '#ef4444' }}>₹{Number(w.writeOffAmount || 0).toLocaleString()}</td>
                      <td style={{ maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{w.reason || '—'}</td>
                      <td>{w.approvedBy || '—'}</td>
                      <td><Badge value={w.status || 'POSTED'} /></td>
                    </tr>
                  ))}
                  {writeoffs.length === 0 && <tr><td colSpan={7}><div className="empty-state"><p>No write-offs recorded</p></div></td></tr>}
                </tbody>
              </table>
            )}

            {tab === 'recoveries' && (
              <table>
                <thead>
                  <tr><th>Recovery ID</th><th>Loan ID</th><th>Customer</th><th>Recovered</th><th>Type</th><th>Date</th><th>Notes</th><th>Status</th></tr>
                </thead>
                <tbody>
                  {recoveries.map(r => (
                    <tr key={r.recoveryId}>
                      <td className="td-mono">{r.recoveryId}</td>
                      <td className="td-primary">#L{r.loanAccountId}</td>
                      <td>{r.customerId}</td>
                      <td style={{ fontWeight: 600, color: '#10b981' }}>₹{Number(r.recoveredAmount || 0).toLocaleString()}</td>
                      <td><Badge value={r.recoveryType || 'CASH'} /></td>
                      <td className="text-muted">{r.recoveryDate || '—'}</td>
                      <td style={{ maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', color: 'var(--muted)', fontSize: 12 }}>{r.notes || '—'}</td>
                      <td><Badge value={r.status || 'PENDING'} /></td>
                    </tr>
                  ))}
                  {recoveries.length === 0 && <tr><td colSpan={8}><div className="empty-state"><p>No recoveries recorded</p></div></td></tr>}
                </tbody>
              </table>
            )}
          </div>
        )}
      </div>

      {/* LEGAL ACTION MODAL */}
      <Modal open={actionModal} onClose={() => { setActionModal(false); setActionForm(emptyAction) }} title="File Legal Action">
        <form onSubmit={saveAction}>
          <div className="modal-body">
            <div className="form-grid">
              <div className="form-group">
                <label className="form-label">Loan Account ID</label>
                <input className="form-input" type="number" placeholder="e.g. 1001" value={actionForm.loanAccountId} onChange={set(setActionForm)('loanAccountId')} required />
              </div>
              <div className="form-group">
                <label className="form-label">Customer ID</label>
                <input className="form-input" type="number" placeholder="e.g. 500" value={actionForm.customerId} onChange={set(setActionForm)('customerId')} required />
              </div>
            </div>
            <div className="form-grid">
              <div className="form-group">
                <label className="form-label">Action Type</label>
                <select className="form-select" value={actionForm.actionType} onChange={set(setActionForm)('actionType')}>
                  {['NOTICE', 'SUMMONS', 'ARBITRATION', 'CIVIL_SUIT', 'CRIMINAL_COMPLAINT', 'LOK_ADALAT'].map(t => <option key={t}>{t}</option>)}
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">Filed Date</label>
                <input className="form-input" type="date" value={actionForm.filedDate} onChange={set(setActionForm)('filedDate')} required />
              </div>
            </div>
            <div className="form-grid">
              <div className="form-group">
                <label className="form-label">Case Number</label>
                <input className="form-input" placeholder="e.g. CC/2024/1234" value={actionForm.caseNumber} onChange={set(setActionForm)('caseNumber')} />
              </div>
              <div className="form-group">
                <label className="form-label">Court Name</label>
                <input className="form-input" placeholder="e.g. Civil Court, Mumbai" value={actionForm.courtName} onChange={set(setActionForm)('courtName')} />
              </div>
            </div>
            <div className="form-group">
              <label className="form-label">Status</label>
              <select className="form-select" value={actionForm.status} onChange={set(setActionForm)('status')}>
                {['OPEN', 'NOTICE_SENT', 'HEARING', 'DECREE', 'DISPOSED'].map(s => <option key={s}>{s}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Notes</label>
              <textarea className="form-textarea" placeholder="Additional remarks…" value={actionForm.notes} onChange={set(setActionForm)('notes')} />
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-outline" onClick={() => setActionModal(false)}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={saving}>{saving ? 'Filing…' : 'File Action'}</button>
          </div>
        </form>
      </Modal>

      {/* WRITE-OFF MODAL */}
      <Modal open={woModal} onClose={() => { setWoModal(false); setWoForm(emptyWriteoff) }} title="Record Write-Off">
        <form onSubmit={saveWriteoff}>
          <div className="modal-body">
            <div className="form-grid">
              <div className="form-group">
                <label className="form-label">Loan Account ID</label>
                <input className="form-input" type="number" placeholder="e.g. 1001" value={woForm.loanAccountId} onChange={set(setWoForm)('loanAccountId')} required />
              </div>
              <div className="form-group">
                <label className="form-label">Customer ID</label>
                <input className="form-input" type="number" placeholder="e.g. 500" value={woForm.customerId} onChange={set(setWoForm)('customerId')} required />
              </div>
            </div>
            <div className="form-group">
              <label className="form-label">Write-Off Amount (₹)</label>
              <input className="form-input" type="number" step="0.01" placeholder="e.g. 50000" value={woForm.writeOffAmount} onChange={set(setWoForm)('writeOffAmount')} required />
            </div>
            <div className="form-group">
              <label className="form-label">Approved By</label>
              <input className="form-input" placeholder="e.g. CFO / Credit Head" value={woForm.approvedBy} onChange={set(setWoForm)('approvedBy')} required />
            </div>
            <div className="form-group">
              <label className="form-label">Status</label>
              <select className="form-select" value={woForm.status} onChange={set(setWoForm)('status')}>
                {['DRAFT', 'PENDING', 'POSTED', 'REVERSED'].map(s => <option key={s}>{s}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Reason</label>
              <textarea className="form-textarea" placeholder="Write-off justification…" value={woForm.reason} onChange={set(setWoForm)('reason')} required />
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-outline" onClick={() => setWoModal(false)}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={saving}>{saving ? 'Saving…' : 'Record Write-Off'}</button>
          </div>
        </form>
      </Modal>

      {/* RECOVERY MODAL */}
      <Modal open={recModal} onClose={() => { setRecModal(false); setRecForm(emptyRecovery) }} title="Record Recovery">
        <form onSubmit={saveRecovery}>
          <div className="modal-body">
            <div className="form-grid">
              <div className="form-group">
                <label className="form-label">Loan Account ID</label>
                <input className="form-input" type="number" placeholder="e.g. 1001" value={recForm.loanAccountId} onChange={set(setRecForm)('loanAccountId')} required />
              </div>
              <div className="form-group">
                <label className="form-label">Customer ID</label>
                <input className="form-input" type="number" placeholder="e.g. 500" value={recForm.customerId} onChange={set(setRecForm)('customerId')} required />
              </div>
            </div>
            <div className="form-grid">
              <div className="form-group">
                <label className="form-label">Recovered Amount (₹)</label>
                <input className="form-input" type="number" step="0.01" placeholder="e.g. 20000" value={recForm.recoveredAmount} onChange={set(setRecForm)('recoveredAmount')} required />
              </div>
              <div className="form-group">
                <label className="form-label">Recovery Date</label>
                <input className="form-input" type="date" value={recForm.recoveryDate} onChange={set(setRecForm)('recoveryDate')} required />
              </div>
            </div>
            <div className="form-group">
              <label className="form-label">Recovery Type</label>
              <select className="form-select" value={recForm.recoveryType} onChange={set(setRecForm)('recoveryType')}>
                {['CASH', 'AUCTION', 'SETTLEMENT', 'COURT_ORDER', 'INSURANCE'].map(t => <option key={t}>{t}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Status</label>
              <select className="form-select" value={recForm.status} onChange={set(setRecForm)('status')}>
                {['PENDING', 'COMPLETED', 'FAILED'].map(s => <option key={s}>{s}</option>)}
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Notes</label>
              <textarea className="form-textarea" placeholder="Recovery details…" value={recForm.notes} onChange={set(setRecForm)('notes')} />
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-outline" onClick={() => setRecModal(false)}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={saving}>{saving ? 'Saving…' : 'Record Recovery'}</button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
