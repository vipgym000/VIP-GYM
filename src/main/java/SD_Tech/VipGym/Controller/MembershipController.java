package SD_Tech.VipGym.Controller;

import SD_Tech.VipGym.Entity.Membership;
import SD_Tech.VipGym.Repository.MembershipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/memberships")
public class MembershipController {

    @Autowired
    private MembershipRepository membershipRepository;

    // Create a new membership plan
    @PostMapping("/add")
    public ResponseEntity<String> addMembership(@RequestBody Membership membership) {
        if (membershipRepository.existsByName(membership.getName())) {
            return ResponseEntity.badRequest().body("Membership plan with this name already exists");
        }

        membershipRepository.save(membership);
        return ResponseEntity.ok("Membership plan added successfully");
    }

    // Get all membership plans
    @GetMapping("/all")
    public List<Membership> getAllMemberships() {
        return membershipRepository.findAll();
    }

    // Get membership by id
    @GetMapping("/{id}")
    public ResponseEntity<Membership> getMembershipById(@PathVariable Long id) {
        return membershipRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Update membership by id
    @PutMapping("/update/{id}")
    public ResponseEntity<String> updateMembership(@PathVariable Long id, @RequestBody Membership updatedMembership) {
        return membershipRepository.findById(id).map(membership -> {
            membership.setName(updatedMembership.getName());
            membership.setDurationInMonths(updatedMembership.getDurationInMonths());
            membership.setFee(updatedMembership.getFee());
            membershipRepository.save(membership);
            return ResponseEntity.ok("Membership updated successfully");
        }).orElse(ResponseEntity.notFound().build());
    }

    // Delete membership by id
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteMembership(@PathVariable Long id) {
        if (!membershipRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        membershipRepository.deleteById(id);
        return ResponseEntity.ok("Membership deleted successfully");
    }
}
