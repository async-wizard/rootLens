import { Routes, Route } from 'react-router-dom';
import { IncidentsPage } from '@/pages/IncidentsPage';
import { IncidentDetailPage } from '@/pages/IncidentDetailPage';
import { ServicesPage } from '@/pages/ServicesPage';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<IncidentsPage />} />
      <Route path="/incidents/:id" element={<IncidentDetailPage />} />
      <Route path="/services" element={<ServicesPage />} />
    </Routes>
  );
}
