package SD_Tech.VipGym.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import SD_Tech.VipGym.Entity.Membership;
import SD_Tech.VipGym.Entity.User;
import SD_Tech.VipGym.Repository.MembershipRepository;
import SD_Tech.VipGym.Repository.UserRepository;

@RestController
@RequestMapping("/admin/memberships")
public class MembershipController {

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private UserRepository userRepository;

    // ➕ Add new membership
    @PostMapping("/add")
    public ResponseEntity<?> addMembership(@RequestBody Membership membership) {
        if (membership.getName() == null || membership.getDurationInMonths() == null || membership.getFee() == null) {
            return ResponseEntity.badRequest().body("Missing required fields: name, durationInMonths, or fee");
        }
        if (membershipRepository.existsByName(membership.getName())) {
            return ResponseEntity.badRequest().body("Membership plan with this name already exists");
        }
        membershipRepository.save(membership);
        return ResponseEntity.ok(Map.of("message", "Membership plan added successfully"));
    }

    // 📋 Get all memberships (without users to avoid recursion)
    @GetMapping("/all")
    public ResponseEntity<List<Membership>> getAllMemberships() {
        List<Membership> memberships = membershipRepository.findAll();
        memberships.forEach(m -> m.setUsers(null)); // prevent recursion
        return ResponseEntity.ok(memberships);
    }

    // 🔍 Get membership by ID
    @GetMapping("/{id}")
    public ResponseEntity<Membership> getMembershipById(@PathVariable Long id) {
        return membershipRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ✏️ Update membership
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateMembership(@PathVariable Long id, @RequestBody Membership updatedMembership) {
        return membershipRepository.findById(id).map(membership -> {
            membership.setName(updatedMembership.getName());
            membership.setDurationInMonths(updatedMembership.getDurationInMonths());
            membership.setFee(updatedMembership.getFee());
            membershipRepository.save(membership);
            return ResponseEntity.ok("Membership updated successfully");
        }).orElse(ResponseEntity.notFound().build());
    }

    // 🗑️ Delete membership safely
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteMembership(@PathVariable Long id) {
        return membershipRepository.findById(id).map(membership -> {
            if (membership.getUsers() != null && !membership.getUsers().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("Cannot delete membership: users are still linked to this plan");
            }
            membershipRepository.delete(membership);
            return ResponseEntity.ok("Membership deleted successfully");
        }).orElse(ResponseEntity.notFound().build());
    }
    
 // 🔄 Switch a user's membership plan
    @PutMapping("/switch/{userId}/{newMembershipId}")
    public ResponseEntity<?> switchUserMembership(
            @PathVariable Long userId,
            @PathVariable Long newMembershipId) {

        try {
            var optionalUser = userRepository.findById(userId);
            if (optionalUser.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("message", "User not found with ID: " + userId));
            }
            User user = optionalUser.get();

            var optionalMembership = membershipRepository.findById(newMembershipId);
            if (optionalMembership.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("message", "New membership plan not found with ID: " + newMembershipId));
            }
            Membership newMembership = optionalMembership.get();

            // ✅ Check same plan
            if (user.getMembership() != null && user.getMembership().getId().equals(newMembershipId)) {
                return ResponseEntity.badRequest().body(Map.of("message", "User is already on this membership plan"));
            }

            // ✅ Check if a switch is already scheduled
            if (user.isMembershipSwitchPending()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "A membership switch is already pending",
                        "nextMembership", user.getNextMembership() != null ? user.getNextMembership().getName() : "N/A"
                ));
            }

            // ✅ Schedule the membership switch
            user.setNextMembership(newMembership);
            user.setMembershipSwitchPending(true);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    "message", "Membership switch scheduled successfully. It will take effect after the current due date.",
                    "userId", user.getId(),
                    "currentMembership", user.getMembership().getName(),
                    "nextMembership", newMembership.getName(),
                    "effectiveAfter", user.getNextDueDate()
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Failed to schedule membership switch: " + e.getMessage()));
        }
    }
}
