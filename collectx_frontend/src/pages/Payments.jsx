import { useState, useEffect } from 'react'
import client from '../api/client'
import Badge from '../components/Badge'
import Modal from '../components/Modal'
import { useToast } from '../components/Toast'
import { useAuth } from '../context/AuthContext'

const emptyPtp        = { loanAccountId: '', agentId: '', customerId: '', promisedAmount: '', promisedDate: '', channel: 'CALL', promisedBy: '' }
const emptyPayment    = { loanAccountId: '', agentId: '', customerId: '', amount: '', paymentMode: 'CASH', referenceNumber: '' }
const emptySettlement = { loanAccountId: '', agentId: '', customerId: '', settlementAmount: '', waiverAmount: '', reason: '' }

export default function Payments() {
  const [tab,         setTab]         = useState(null)
  const [ptps,        setPtps]        = useState([])
  const [payments,    setPayments]    = useState([])
  const [settlements, setSettlements] = useState([])
  const [loading,     setLoading]     = useState(true)
  const [ptpModal,    setPtpModal]    = useState(false)
  const [payModal,    setPayModal]    = useState(false)
  const [settModal,   setSettModal]   = useState(false)
  const [ptpForm,     setPtpForm]     = useState(emptyPtp)
  const [payForm,     setPayForm]     = useState(emptyPayment)
  const [settForm,    setSettForm]    = useState(emptySettlement)
  const [saving,      setSaving]      = useState(false)
  const toast = useToast()
  const { user } = useAuth()
  const canSettle          = ['SUPERVISOR', 'ADMIN'].includes(user?.role)
  const canRecord          = ['ADMIN', 'SUPERVISOR', 'AGENT'].includes(user?.role)
  const canLogPtp          = ['ADMIN', 'SUPERVISOR', 'AGENT', 'FIELD', 'RECOVERY'].includes(user?.role)
  const canViewPtps        = ['ADMIN', 'SUPERVISOR', 'AGENT', 'COMPLIANCE'].includes(user?.role)
  const canViewSettlements = ['ADMIN', 'SUPERVISOR', 'COMPLIANCE'].includes(user?.role)
  
  const canApproveSettlement = ['ADMIN', 'SUPERVISOR'].includes(user?.role)
  const activeTab = tab ?? (canViewPtps ? 'ptps' : 'payments')

  const loadAll = () => {
    setLoading(true)
    Promise.allSettled([
      canViewPtps ? client.get('/payment/ptp/all') : Promise.resolve({ data: [] }),
      client.get('/payment/payments'),
      canViewSettlements ? client.get('/payment/settlements') : Promise.resolve({ data: [] }),
    ]).then(([p, py, s]) => {
      setPtps(p.status === 'fulfilled' ? p.value.data || [] : [])
      setPayments(py.status === 'fulfilled' ? py.value.data || [] : [])
      setSettlements(s.status === 'fulfilled' ? s.value.data || [] : [])
    }).finally(() => setLoading(false))
  }

  useEffect(loadAll, [])

  const set = setter => k => e => setter(f => ({ ...f, [k]: e.target.value }))

  const savePtp = async e => {
    e.preventDefault()
    setSaving(true)
    try {
      const res = await client.post('/payment/ptp', {
        loanAccountId: Number(ptpForm.loanAccountId),
        agentId: Number(ptpForm.agentId),
        customerId: Number(ptpForm.customerId),
        promisedAmount: Number(ptpForm.promisedAmount),
        promisedDate: ptpForm.promisedDate,
        channel: ptpForm.channel,
        promisedBy: ptpForm.promisedBy,
      })
      setPtps(prev => [res.data, ...prev])
      setTab('ptps')
      toast('PTP logged', 'success')
      setPtpModal(false)
      setPtpForm(emptyPtp)
      loadAll()
    } catch (err) {
      toast(err.response?.data?.message || 'Failed to log PTP', 'error')
    } finally { setSaving(false) }
  }

  const savePayment = async e => {
    e.preventDefault()
    setSaving(true)
    try {
      const res = await client.post('/payment/create', {
        loanAccountId: Number(payForm.loanAccountId),
        agentId: Number(payForm.agentId),
        customerId: Number(payForm.customerId),
        amount: Number(payForm.amount),
        paymentMode: payForm.paymentMode,
        referenceNumber: payForm.referenceNumber,
      })
      const newPayment = res.data
      setTab('payments')
      toast('Payment recorded', 'success')
      setPayModal(false)
      setPayForm(emptyPayment)
     
      client.get('/payment/payments')
        .then(r => setPayments(r.data?.length ? r.data : prev => [newPayment, ...prev]))
        .catch(() => setPayments(prev => [newPayment, ...prev]))
      client.get('/payment/ptp/all').then(r => setPtps(r.data || [])).catch(() => {})
    } catch (err) {
      toast(err.response?.data?.message || 'Failed to record payment', 'error')
    } finally { setSaving(false) }
  }

  const saveSettlement = async e => {
    e.preventDefault()
    setSaving(true)
    try {
      const res = await client.post('/payment/settlement', {
        loanAccountId: Number(settForm.loanAccountId),
        agentId: Number(settForm.agentId),
        customerId: Number(settForm.customerId),
        settlementAmount: Number(settForm.settlementAmount),
        waiverAmount: Number(settForm.waiverAmount),
        reason: settForm.reason,
      })
      setSettlements(prev => [res.data, ...prev])
      setTab('settlements')
      toast('Settlement request submitted', 'success')
      setSettModal(false)
      setSettForm(emptySettlement)
      loadAll()
    } catch (err) {
      toast(err.response?.data?.message || 'Failed to submit settlement', 'error')
    } finally { setSaving(false) }
  }

  
  const handleSettlementDecision = async (settlementId, decision) => {
    try {
      const res = await client.patch(`/payment/settlement/${settlementId}/approve?decision=${decision}`)
      setSettlements(prev => prev.map(s => s.settlementId === settlementId ? res.data : s))
      toast(`Settlement ${decision === 'APPROVED' ? 'approved ✅' : 'rejected ❌'}`, decision === 'APPROVED' ? 'success' : 'warning')
    } catch (err) {
      toast(err.response?.data?.message || 'Failed to update settlement', 'error')
    }
  }

  const totalCollected = payments.reduce((s, p) => s + (p.amount || 0), 0)
  const keptPtps  = ptps.filter(p => p.status === 'KEPT').length
  const pendPtps  = ptps.filter(p => p.status === 'OPEN' || p.status === 'INITIATED').length
  const approvedS = settlements.filter(s => s.approvalStatus === 'APPROVED').length

  return (
    <div>
      <div className="page-header">
        <div>
          <div className="page-title">Payments & Settlements</div>
          <div className="page-subtitle">Manage PTPs, record payments and track settlement requests</div>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          {canSettle && (
            <button className="btn btn-outline" onClick={() => setSettModal(true)}>+ Settlement</button>
          )}
          {activeTab === 'ptps' && canLogPtp && (
            <button className="btn btn-outline" onClick={() => setPtpModal(true)}>+ Log PTP</button>
          )}
          {canRecord && (
            <button className="btn btn-primary" onClick={() => setPayModal(true)}>+ Record Payment</button>
          )}
        </div>
      </div>

      <div className="stats-grid" style={{ marginBottom: 20 }}>
        <div className="stat-card">
          <div className="stat-label">Total Collected</div>
          <div className="stat-value">₹{totalCollected.toLocaleString()}</div>
        </div>
        <div className="stat-card" style={{ borderLeft: '4px solid #10b981' }}>
          <div className="stat-label">PTPs Kept</div>
          <div className="stat-value">{keptPtps}</div>
        </div>
        <div className="stat-card" style={{ borderLeft: '4px solid #f59e0b' }}>
          <div className="stat-label">Pending PTPs</div>
          <div className="stat-value">{pendPtps}</div>
        </div>
        <div className="stat-card" style={{ borderLeft: '4px solid #6366f1' }}>
          <div className="stat-label">Settlements Approved</div>
          <div className="stat-value">{approvedS}</div>
        </div>
      </div>

      <div className="tabs">
        {canViewPtps && <button className={`tab-btn ${activeTab === 'ptps' ? 'active' : ''}`} onClick={() => setTab('ptps')}>PTPs ({ptps.length})</button>}
        <button className={`tab-btn ${activeTab === 'payments' ? 'active' : ''}`} onClick={() => setTab('payments')}>Payments ({payments.length})</button>
        {canViewSettlements && <button className={`tab-btn ${activeTab === 'settlements' ? 'active' : ''}`} onClick={() => setTab('settlements')}>Settlements ({settlements.length})</button>}
      </div>

      <div className="card">
        <div className="card-header">
          <span className="card-title">
            {activeTab === 'ptps' ? 'Promise to Pay' : activeTab === 'payments' ? 'Payment Records' : 'Settlement Requests'}
          </span>
          <button className="btn btn-ghost btn-sm" onClick={loadAll}>↻ Refresh</button>
        </div>

        {loading ? <div className="loader-wrap"><div className="spinner" /></div> : (
          <div className="table-wrapper">
            {activeTab === 'ptps' && (
              <table>
                <thead>
                  <tr><th>PTP ID</th><th>Loan ID</th><th>Agent</th><th>Customer</th><th>Amount</th><th>Promised Date</th><th>Channel</th><th>Status</th></tr>
                </thead>
                <tbody>
                  {ptps.map(p => (
                    <tr key={p.ptpId}>
                      <td className="td-mono">{p.ptpId}</td>
                      <td className="td-primary">#L{p.loanAccountId}</td>
                      <td>Agent {p.agentId}</td>
                      <td>{p.customerId}</td>
                      <td style={{ fontWeight: 600 }}>₹{Number(p.promisedAmount || 0).toLocaleString()}</td>
                      <td className="text-muted">{p.promisedDate || '—'}</td>
                      <td><Badge value={p.channel} /></td>
                      <td><Badge value={p.status} /></td>
                    </tr>
                  ))}
                  {ptps.length === 0 && <tr><td colSpan={8}><div className="empty-state"><p>No PTPs recorded yet</p></div></td></tr>}
                </tbody>
              </table>
            )}

            {activeTab === 'payments' && (
              <table>
                <thead>
                  <tr><th>Payment ID</th><th>Loan ID</th><th>Customer</th><th>Amount</th><th>Mode</th><th>Reference</th><th>Date</th><th>Status</th></tr>
                </thead>
                <tbody>
                  {payments.map(p => (
                    <tr key={p.paymentId}>
                      <td className="td-mono">{p.paymentId}</td>
                      <td className="td-primary">#L{p.loanAccountId}</td>
                      <td>{p.customerId}</td>
                      <td style={{ fontWeight: 600, color: '#10b981' }}>₹{Number(p.amount || 0).toLocaleString()}</td>
                      <td><Badge value={p.paymentMode} /></td>
                      <td className="td-mono" style={{ fontSize: 12 }}>{p.referenceNumber || '—'}</td>
                      <td className="text-muted">{p.paymentDate ? new Date(p.paymentDate).toLocaleString() : '—'}</td>
                      <td><Badge value={p.status || 'POSTED'} /></td>
                    </tr>
                  ))}
                  {payments.length === 0 && <tr><td colSpan={8}><div className="empty-state"><p>No payments recorded yet</p></div></td></tr>}
                </tbody>
              </table>
            )}

            {activeTab === 'settlements' && (
              <table>
                <thead>
                 
                  <tr><th>Settlement ID</th><th>Loan ID</th><th>Customer</th><th>Settlement Amt</th><th>Waiver Amt</th><th>Reason</th><th>Status</th>{canApproveSettlement && <th>Action</th>}</tr>
                </thead>
                <tbody>
                  {settlements.map(s => (
                    <tr key={s.settlementId}>
                      <td className="td-mono">{s.settlementId}</td>
                      <td className="td-primary">#L{s.loanAccountId}</td>
                      <td>{s.customerId}</td>
                      <td style={{ fontWeight: 600 }}>₹{Number(s.settlementAmount || 0).toLocaleString()}</td>
                      <td style={{ color: '#f59e0b' }}>₹{Number(s.waiverAmount || 0).toLocaleString()}</td>
                      <td style={{ maxWidth: 200, whiteSpace: 'pre-wrap' }}>{s.reason || '—'}</td>
                      <td><Badge value={s.approvalStatus || s.status} /></td>
                  
                      {canApproveSettlement && (
                        <td>
                          {s.approvalStatus === 'REQUESTED' ? (
                            <div style={{ display: 'flex', gap: 6 }}>
                              <button
                                className="btn btn-sm"
                                style={{ background: '#10b981', color: '#fff', border: 'none', padding: '4px 10px', borderRadius: 6, cursor: 'pointer', fontSize: 12, fontWeight: 600 }}
                                onClick={() => handleSettlementDecision(s.settlementId, 'APPROVED')}
                              >
                                ✓ Approve
                              </button>
                              <button
                                className="btn btn-sm"
                                style={{ background: '#ef4444', color: '#fff', border: 'none', padding: '4px 10px', borderRadius: 6, cursor: 'pointer', fontSize: 12, fontWeight: 600 }}
                                onClick={() => handleSettlementDecision(s.settlementId, 'REJECTED')}
                              >
                                ✗ Reject
                              </button>
                            </div>
                          ) : (
                            <span style={{ fontSize: 12, color: '#9ca3af' }}>—</span>
                          )}
                        </td>
                      )}
                    </tr>
                  ))}
                  {settlements.length === 0 && <tr><td colSpan={canApproveSettlement ? 8 : 7}><div className="empty-state"><p>No settlements yet</p></div></td></tr>}
                </tbody>
              </table>
            )}
          </div>
        )}
      </div>


      {/* PTP MODAL */}
      <Modal open={ptpModal} onClose={() => { setPtpModal(false); setPtpForm(emptyPtp) }} title="Log Promise to Pay">
        <form onSubmit={savePtp}>
          <div className="modal-body">
            <div className="form-grid">
              <div className="form-group">
                <label className="form-label">Loan Account ID</label>
                <input className="form-input" type="number" placeholder="e.g. 1001" value={ptpForm.loanAccountId} onChange={set(setPtpForm)('loanAccountId')} required />
              </div>
              <div className="form-group">
                <label className="form-label">Customer ID</label>
                <input className="form-input" type="number" placeholder="e.g. 500" value={ptpForm.customerId} onChange={set(setPtpForm)('customerId')} required />
              </div>
            </div>
            <div className="form-grid">
              <div className="form-group">
                <label className="form-label">Agent ID</label>
                <input className="form-input" type="number" placeholder="e.g. 1" value={ptpForm.agentId} onChange={set(setPtpForm)('agentId')} required />
              </div>
              <div className="form-group">
                <label className="form-label">Promised Amount (₹)</label>
                <input className="form-input" type="number" step="0.01" placeholder="e.g. 10000" value={ptpForm.promisedAmount} onChange={set(setPtpForm)('promisedAmount')} required />
              </div>
            </div>
            <div className="form-grid">
              <div className="form-group">
                <label className="form-label">Promise Date</label>
                <input className="form-input" type="date" value={ptpForm.promisedDate} onChange={set(setPtpForm)('promisedDate')} required />
              </div>
              <div className="form-group">
                <label className="form-label">Channel</label>
                <select className="form-select" value={ptpForm.channel} onChange={set(setPtpForm)('channel')}>
                  {['CALL', 'SMS', 'EMAIL', 'VISIT'].map(c => <option key={c}>{c}</option>)}
                </select>
              </div>
            </div>
            <div className="form-group">
              <label className="form-label">Promised By</label>
              <input className="form-input" placeholder="Customer name / contact" value={ptpForm.promisedBy} onChange={set(setPtpForm)('promisedBy')} />
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-outline" onClick={() => setPtpModal(false)}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={saving}>{saving ? 'Saving…' : 'Log PTP'}</button>
          </div>
        </form>
      </Modal>

      {/* PAYMENT MODAL */}
      <Modal open={payModal} onClose={() => { setPayModal(false); setPayForm(emptyPayment) }} title="Record Payment">
        <form onSubmit={savePayment}>
          <div className="modal-body">
            <div className="form-grid">
              <div className="form-group">
                <label className="form-label">Loan Account ID</label>
                <input className="form-input" type="number" placeholder="e.g. 1001" value={payForm.loanAccountId} onChange={set(setPayForm)('loanAccountId')} required />
              </div>
              <div className="form-group">
                <label className="form-label">Customer ID</label>
                <input className="form-input" type="number" placeholder="e.g. 500" value={payForm.customerId} onChange={set(setPayForm)('customerId')} required />
              </div>
            </div>
            <div className="form-grid">
              <div className="form-group">
                <label className="form-label">Agent ID</label>
                <input className="form-input" type="number" placeholder="e.g. 1" value={payForm.agentId} onChange={set(setPayForm)('agentId')} required />
              </div>
              <div className="form-group">
                <label className="form-label">Amount (₹)</label>
                <input className="form-input" type="number" step="0.01" placeholder="e.g. 10000" value={payForm.amount} onChange={set(setPayForm)('amount')} required />
              </div>
            </div>
            <div className="form-grid">
              <div className="form-group">
                <label className="form-label">Payment Mode</label>
                <select className="form-select" value={payForm.paymentMode} onChange={set(setPayForm)('paymentMode')}>
                  {['CASH', 'CHEQUE', 'UPI', 'NEFT', 'RTGS'].map(m => <option key={m}>{m}</option>)}
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">Reference Number</label>
                <input className="form-input" placeholder="UTR / Cheque No." value={payForm.referenceNumber} onChange={set(setPayForm)('referenceNumber')} />
              </div>
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-outline" onClick={() => setPayModal(false)}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={saving}>{saving ? 'Saving…' : 'Record Payment'}</button>
          </div>
        </form>
      </Modal>

      {/* SETTLEMENT MODAL */}
      <Modal open={settModal} onClose={() => { setSettModal(false); setSettForm(emptySettlement) }} title="Request Settlement">
        <form onSubmit={saveSettlement}>
          <div className="modal-body">
            <div className="form-grid">
              <div className="form-group">
                <label className="form-label">Loan Account ID</label>
                <input className="form-input" type="number" placeholder="e.g. 1001" value={settForm.loanAccountId} onChange={set(setSettForm)('loanAccountId')} required />
              </div>
              <div className="form-group">
                <label className="form-label">Customer ID</label>
                <input className="form-input" type="number" placeholder="e.g. 500" value={settForm.customerId} onChange={set(setSettForm)('customerId')} required />
              </div>
            </div>
            <div className="form-group">
              <label className="form-label">Agent ID</label>
              <input className="form-input" type="number" placeholder="e.g. 1" value={settForm.agentId} onChange={set(setSettForm)('agentId')} required />
            </div>
            <div className="form-grid">
              <div className="form-group">
                <label className="form-label">Settlement Amount (₹)</label>
                <input className="form-input" type="number" step="0.01" placeholder="e.g. 45000" value={settForm.settlementAmount} onChange={set(setSettForm)('settlementAmount')} required />
              </div>
              <div className="form-group">
                <label className="form-label">Waiver Amount (₹)</label>
                <input className="form-input" type="number" step="0.01" placeholder="e.g. 5000" value={settForm.waiverAmount} onChange={set(setSettForm)('waiverAmount')} required />
              </div>
            </div>
            <div className="form-group">
              <label className="form-label">Reason</label>
              <textarea className="form-textarea" placeholder="Reason for settlement…" value={settForm.reason} onChange={set(setSettForm)('reason')} required />
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-outline" onClick={() => setSettModal(false)}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={saving}>{saving ? 'Submitting…' : 'Submit Request'}</button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
