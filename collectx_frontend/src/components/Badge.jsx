const map = {
 
  OPEN:        'blue',   ACTIVE:      'green',  CLOSED:    'gray',
  RESOLVED:    'green',  INPROGRESS:  'purple', IN_PROGRESS:'purple',
  PENDING:     'yellow', PROCESSING:  'blue',

 
  KEPT:        'green',  BROKEN:      'red',    POSTED:     'green',
  FAILED:      'red',    REQUESTED:   'yellow', APPROVED:   'green',
  REJECTED:    'red',    COMPLETED:   'green',  CANCELLED:  'gray',
  PAID:        'green',  PARTIAL:     'yellow', OVERDUE:    'red',


  INITIATED:   'blue',   DISPOSED:    'gray',   REVERSED:   'orange',
  EXPIRED:     'gray',   HONORED:     'green',  DEFAULTED:  'red',


  EMPANELED:   'green',  SUSPENDED:   'red',
  SEIZED:      'red',    STORED:      'yellow', AUCTIONED:  'gray',
  RELEASED:    'green',

  
  CONNECTED:      'green',  NO_ANSWER:   'yellow',
  REFUSED:        'red',    PARTIAL_PAYMENT: 'teal',
  PTP_GIVEN:      'blue',   NOT_HOME:    'yellow',

  
  '0-30':      'green',  '31-60':     'yellow',
  '61-90':     'orange', '90+':       'red',


  LOW:         'green',  MED:         'yellow', HIGH:       'red',
  MEDIUM:      'yellow', CRITICAL:    'red',

  CURRENT:     'green',  DELINQUENT:  'yellow', NPA:        'red',


  CALL:        'blue',   SMS:         'teal',   EMAIL:      'purple',
  VISIT:       'orange', INAPP:       'gray',

 
  UNREAD:      'blue',   READ:        'gray',   DISMISSED:  'gray',

  ALLOWED:     'green',  OPTOUT:      'red',


  EMPANELLED:  'green',


  ADMIN:       'red',    SUPERVISOR: 'blue',  AGENT:      'green',
  FIELD:       'yellow', RECOVERY:   'orange', COMPLIANCE: 'purple',

 
  INACTIVE:    'gray',   LOCKED:     'red',
}

export default function Badge({ value }) {
  const key   = String(value ?? '').toUpperCase().replace(/\s+/g, '_')
  const color = map[key] || 'gray'
  return (
    <span className={`badge badge-${color}`}>
      {String(value ?? '').replace(/_/g, ' ')}
    </span>
  )
}
