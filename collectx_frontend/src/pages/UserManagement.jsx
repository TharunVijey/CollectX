import { useState, useEffect } from 'react'
import client from '../api/client'
import Badge from '../components/Badge'
import Modal from '../components/Modal'
import { useToast } from '../components/Toast'
import { useAuth } from '../context/AuthContext'

const ROLES    = [ 'SUPERVISOR', 'AGENT', 'FIELD', 'RECOVERY', 'COMPLIANCE']
const STATUSES = ['ACTIVE', 'INACTIVE', 'LOCKED']

const emptyCreate = { name: '', email: '', password: '', role: 'AGENT', phone: '' }
const emptyEdit   = { name: '', email: '', password: '', role: 'AGENT', status: 'ACTIVE', phone: '' }

export default function UserManagement() {
  const [tab,         setTab]         = useState('users')
  const [users,        setUsers]       = useState([])
  const [auditLogs,    setAuditLogs]   = useState([])
  const [loading,      setLoading]     = useState(true)
  const [auditLoading, setAuditLoading]= useState(false)
  const [search,       setSearch]      = useState('')
  const [filterRole,   setFilterRole]  = useState('ALL')
  const [createModal,  setCreateModal] = useState(false)
  const [editModal,    setEditModal]   = useState(false)
  const [deleteModal,  setDeleteModal] = useState(false)
  const [selected,     setSelected]    = useState(null)
  const [createForm,   setCreateForm]  = useState(emptyCreate)
  const [editForm,     setEditForm]    = useState(emptyEdit)
  const [saving,       setSaving]      = useState(false)
  const toast = useToast()
  const { user } = useAuth()
  const isAdmin = user?.role === 'ADMIN'

  const load = () => {
    if (!isAdmin) { setLoading(false); return }
    setLoading(true)
    client.get('/admin/users')
      .then(r => setUsers(r.data || []))
      .catch(() => toast('Could not load users', 'error'))
      .finally(() => setLoading(false))
  }

  const loadAuditLogs = () => {
    setAuditLoading(true)
    client.get('/admin/audit-logs')
      .then(r => setAuditLogs(r.data || []))
      .catch(() => toast('Could not load audit logs', 'error'))
      .finally(() => setAuditLoading(false))
  }

  useEffect(() => { load(); loadAuditLogs() }, [])
  useEffect(() => { if (tab === 'audit') loadAuditLogs() }, [tab])

  const setC = k => e => setCreateForm(f => ({ ...f, [k]: e.target.value }))
  const setE = k => e => setEditForm(f => ({ ...f, [k]: e.target.value }))

  const handleCreate = async e => {
    e.preventDefault()
    if (!createForm.name || !createForm.email || !createForm.password)
      return toast('Please fill all required fields', 'warning')
    if (createForm.password.length < 8)
      return toast('Password must be at least 8 characters', 'warning')
    setSaving(true)
    try {
      await client.post('/admin/users', {
        name:     createForm.name,
        email:    createForm.email,
        password: createForm.password,
        role:     createForm.role,
        phone:    createForm.phone || null,
      })
      toast('User created successfully', 'success')
      setCreateModal(false)
      setCreateForm(emptyCreate)
      load()
    } catch (err) {
      toast(err.response?.data?.message || 'Failed to create user', 'error')
    } finally { setSaving(false) }
  }

  const openEdit = u => {
    setSelected(u)
    setEditForm({ name: u.name, email: u.email, password: '', role: u.role, status: u.status, phone: u.phone || '' })
    setEditModal(true)
  }

  const handleEdit = async e => {
    e.preventDefault()
    if (!editForm.name || !editForm.email) return toast('Name and email are required', 'warning')
    if (editForm.password && editForm.password.length < 8)
      return toast('Password must be at least 8 characters', 'warning')
    setSaving(true)
    try {
      await client.put(`/admin/users/${selected.userId}`, {
        name:     editForm.name,
        email:    editForm.email,
        role:     editForm.role,
        status:   editForm.status,
        password: editForm.password || null,
        phone:    editForm.phone || null,
      })
      toast('User updated', 'success')
      setEditModal(false)
      load()
    } catch (err) {
      toast(err.response?.data?.message || 'Failed to update user', 'error')
    } finally { setSaving(false) }
  }

  const openDelete = u => { setSelected(u); setDeleteModal(true) }

  const handleDelete = async () => {
    setSaving(true)
    try {
      await client.delete(`/admin/users/${selected.userId}`)
      toast('User removed', 'success')
      setDeleteModal(false)
      load()
    } catch (err) {
      toast(err.response?.data?.message || 'Failed to remove user', 'error')
    } finally { setSaving(false) }
  }

  const filtered = users.filter(u => {
    const matchRole   = filterRole === 'ALL' || u.role === filterRole
    const q           = search.toLowerCase()
    const matchSearch = !q || u.name?.toLowerCase().includes(q) || u.email?.toLowerCase().includes(q)
    return matchRole && matchSearch
  })

 
  if (!isAdmin) return null

  return (
    <div className="page-inner">

      {/* ── Header ── */}
      <div className="page-header">
        <div>
          <div className="page-title">User Management</div>
          <div className="page-subtitle">{users.length} user{users.length !== 1 ? 's' : ''} in the system</div>
        </div>
        {tab === 'users' && (
          <button className="btn btn-primary" onClick={() => setCreateModal(true)}>+ Create User</button>
        )}
      </div>

      {/* ── Tabs ── */}
      <div className="tabs" style={{ marginBottom: 16 }}>
        <button className={`tab-btn ${tab === 'users' ? 'active' : ''}`} onClick={() => setTab('users')}>
          Users ({users.length})
        </button>
        <button className={`tab-btn ${tab === 'audit' ? 'active' : ''}`} onClick={() => setTab('audit')}>
          Audit Log ({auditLogs.length})
        </button>
      </div>

      {/* ── USERS TAB ── */}
      {tab === 'users' && (
        <>
          {/* Filters */}
          <div className="filter-bar">
            <input
              className="search-input"
              placeholder="Search by name or email…"
              value={search}
              onChange={e => setSearch(e.target.value)}
            />
            <select className="filter-select" value={filterRole} onChange={e => setFilterRole(e.target.value)}>
              <option value="ALL">All Roles</option>
              {ROLES.map(r => <option key={r} value={r}>{r}</option>)}
            </select>
          </div>

          {/* Table */}
          <div className="table-wrap">
            {loading ? (
              <div className="empty-state">Loading users…</div>
            ) : filtered.length === 0 ? (
              <div className="empty-state">No users found.</div>
            ) : (
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Email</th>
                    <th>Phone</th>
                    <th>Role</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.map(u => (
                    <tr key={u.userId}>
                      <td>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                          <div style={{
                            width: 32, height: 32, borderRadius: '50%',
                            background: 'var(--accent-bg)', color: 'var(--accent)',
                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                            fontWeight: 700, fontSize: 13, flexShrink: 0,
                          }}>
                            {u.name?.[0]?.toUpperCase() || '?'}
                          </div>
                          <span style={{ fontWeight: 600 }}>{u.name}</span>
                        </div>
                      </td>
                      <td style={{ color: 'var(--text-2)', fontSize: 13 }}>{u.email}</td>
                      <td style={{ color: 'var(--text-2)', fontSize: 13 }}>{u.phone || '—'}</td>
                      <td><Badge value={u.role} /></td>
                      <td><Badge value={u.status} /></td>
                      <td>
                        <div style={{ display: 'flex', gap: 6 }}>
                          <button
                            className="btn btn-outline"
                            style={{ padding: '4px 12px', fontSize: 12 }}
                            onClick={() => openEdit(u)}
                            disabled={u.userId === user?.userId}
                          >
                            Edit
                          </button>
                          <button
                            className="btn btn-danger"
                            style={{ padding: '4px 12px', fontSize: 12 }}
                            onClick={() => openDelete(u)}
                            disabled={u.email === user?.email}
                          >
                            Remove
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </>
      )}

      {/* ── AUDIT LOG TAB ── */}
      {tab === 'audit' && (
        <div className="card">
          <div className="card-header">
            <span className="card-title">System Audit Log</span>
            <button className="btn btn-ghost btn-sm" onClick={loadAuditLogs}>↻ Refresh</button>
          </div>
          {auditLoading ? <div className="loader-wrap"><div className="spinner" /></div> : (
            <div className="table-wrapper">
              <table>
                <thead>
                  <tr><th>Log ID</th><th>Action</th><th>Performed By</th><th>Target User</th><th>Details</th><th>Timestamp</th></tr>
                </thead>
                <tbody>
                  {auditLogs.map(log => (
                    <tr key={log.logId}>
                      <td className="td-mono">{log.logId}</td>
                      <td><Badge value={log.action} /></td>
                      <td style={{ fontSize: 13 }}>{log.performedBy || '—'}</td>
                      <td style={{ fontSize: 13 }}>{log.targetEmail || '—'}</td>
                      <td style={{ fontSize: 12, color: 'var(--muted)', maxWidth: 240, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{log.details || '—'}</td>
                      <td className="text-muted">{log.createdAt ? new Date(log.createdAt).toLocaleString() : '—'}</td>
                    </tr>
                  ))}
                  {auditLogs.length === 0 && <tr><td colSpan={6}><div className="empty-state"><p>No audit logs yet</p><span>Logs are created when users are created, updated or deleted</span></div></td></tr>}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* ── Create Modal ── */}
      {createModal && (
        <Modal open title="Create New User" onClose={() => setCreateModal(false)}>
          <form onSubmit={handleCreate}>
            <div className="modal-body">
              <div className="form-grid">
                <div className="form-field">
                  <label className="form-label">Role</label>
                  <select className="form-select" value={createForm.role} onChange={setC('role')}>
                    {ROLES.map(r => <option key={r} value={r}>{r}</option>)}
                  </select>
                </div>
                <div className="form-field">
                  <label className="form-label">Full Name</label>
                  <input className="form-input" placeholder="Riya Sharma" value={createForm.name} onChange={setC('name')} required />
                </div>
                <div className="form-field">
                  <label className="form-label">Email</label>
                  <input className="form-input" type="email" placeholder="riya@collectx.in" value={createForm.email} onChange={setC('email')} required />
                </div>
                <div className="form-field">
                  <label className="form-label">Phone</label>
                  <input className="form-input" type="tel" placeholder="9876543210" value={createForm.phone} onChange={setC('phone')} />
                </div>
                <div className="form-field" style={{ gridColumn: '1 / -1' }}>
                  <label className="form-label">Password <span style={{ color: 'var(--text-3)', fontWeight: 400, fontSize: 11 }}>(min. 8 characters)</span></label>
                  <input className="form-input" type="password" placeholder="••••••••" value={createForm.password} onChange={setC('password')} required minLength={8} />
                </div>
              </div>
            </div>
            <div className="modal-footer">
              <button type="button" className="btn btn-outline" onClick={() => setCreateModal(false)}>Cancel</button>
              <button type="submit" className="btn btn-primary" disabled={saving}>
                {saving ? 'Creating…' : 'Create User'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* ── Edit Modal ── */}
      {editModal && selected && (
        <Modal open title={`Edit — ${selected.name}`} onClose={() => setEditModal(false)}>
          <form onSubmit={handleEdit}>
            <div className="modal-body">
              <div className="form-grid">
                <div className="form-field">
                  <label className="form-label">Full Name</label>
                  <input className="form-input" value={editForm.name} onChange={setE('name')} required />
                </div>
                <div className="form-field">
                  <label className="form-label">Email</label>
                  <input className="form-input" type="email" value={editForm.email} onChange={setE('email')} required />
                </div>
                <div className="form-field">
                  <label className="form-label">Phone</label>
                  <input className="form-input" type="tel" placeholder="9876543210" value={editForm.phone} onChange={setE('phone')} />
                </div>
                <div className="form-field">
                  <label className="form-label">Role</label>
                  <select className="form-select" value={editForm.role} onChange={setE('role')}>
                    {ROLES.map(r => <option key={r} value={r}>{r}</option>)}
                  </select>
                </div>
                <div className="form-field">
                  <label className="form-label">Status</label>
                  <select className="form-select" value={editForm.status} onChange={setE('status')}>
                    {STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
                  </select>
                </div>
                <div className="form-field" style={{ gridColumn: '1 / -1' }}>
                  <label className="form-label">New Password <span style={{ color: 'var(--text-3)', fontWeight: 400, fontSize: 11 }}>(leave blank to keep current)</span></label>
                  <input className="form-input" type="password" placeholder="••••••••" value={editForm.password} onChange={setE('password')} minLength={8} />
                </div>
              </div>
            </div>
            <div className="modal-footer">
              <button type="button" className="btn btn-outline" onClick={() => setEditModal(false)}>Cancel</button>
              <button type="submit" className="btn btn-primary" disabled={saving}>
                {saving ? 'Saving…' : 'Save Changes'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* ── Delete Confirm Modal ── */}
      {deleteModal && selected && (
        <div className="modal-overlay" onClick={() => setDeleteModal(false)}>
          <div className="modal" style={{ maxWidth: 380 }} onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <span className="modal-title">Remove User</span>
              <button className="modal-close" onClick={() => setDeleteModal(false)}>×</button>
            </div>
            <div className="modal-body" style={{ textAlign: 'center', paddingTop: 28, paddingBottom: 6 }}>
              <p style={{ fontSize: 15, fontWeight: 700, marginBottom: 6 }}>Remove {selected.name}?</p>
              <p style={{ fontSize: 13, color: 'var(--text-3)', lineHeight: 1.5 }}>
                This will permanently delete the account for <strong>{selected.email}</strong>. This action cannot be undone.
              </p>
            </div>
            <div className="modal-footer" style={{ justifyContent: 'center', gap: 10 }}>
              <button className="btn btn-outline" onClick={() => setDeleteModal(false)}>Cancel</button>
              <button className="btn btn-danger" onClick={handleDelete} disabled={saving}>
                {saving ? 'Removing…' : 'Remove User'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
